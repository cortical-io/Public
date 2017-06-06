package io.cortical.iris;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.cortical.iris.message.CompareRequest;
import io.cortical.iris.message.CompareResponse;
import io.cortical.iris.message.CompoundTermException;
import io.cortical.iris.message.EmptyFingerprintException;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.message.ServerResponse;
import io.cortical.iris.window.Window;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.core.Terms;
import io.cortical.retina.model.Context;
import io.cortical.retina.model.Fingerprint;
import io.cortical.retina.model.Language;
import io.cortical.retina.model.Metric;
import io.cortical.retina.model.Term;
import io.cortical.retina.model.Text;
import io.cortical.util.Deque;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;


/**
 * Wrapper for the {@link FullClient} which delegates to a cache lookup to
 * retrieve query results for previously identical requests.
 */
public class CachingClientWrapper {
    private static final long DEFAULT_REQUEST_TIMEOUT = 30000L; // 1hour for debug // 30 secs.
    
    private Map<ServerRequest, ServerResponse> requestCache = new HashMap<>();
    private Deque<ServerRequest> sizeAndOrderTracker = new Deque<>(20); 

    /** Stores the terms received from server side queries */
    private Map<String, Term> termCache = Collections.synchronizedMap(new HashMap<>());

    /** Stores the positions to similar terms previously retrieved */
    private Map<LanguageKey, ServerResponse> positionLookupCache = new ConcurrentHashMap<>();


    /**
     * Creates a new {@code CachingClientWrapper}
     * 
     * @param fullClientWrapper
     * @param extendedClientWrapper
     */
    public CachingClientWrapper() {}

    /**
     * Sends messages to the Retina API server.
     * 
     * Note: Exceptions thrown at or after this method are handled via the 
     * returned {@link Observable} {@link Subscriber}'s {@link Subscriber#onError(Throwable)}
     * method.
     * 
     * @param request
     * @return
     */
    public Observable<ServerResponse> getServerResponseObservable(ServerRequest request) {
        Observable<ServerResponse> obs = Observable.create(s -> {
            getServerResponse(request, s);
        });
        // Add a timeout observable to the pipeline to handle extraordinary delays
        Observable<ServerResponse> retVal = obs.timeout(DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS);

        return retVal;
    }

    /**
     * Queries the back end for a similar terms by position lookup.
     * @param request
     * @return
     */
    public Observable<ServerResponse> getExtendedServerResponseObservable(ServerRequest request) {
        Observable<ServerResponse> obs = Observable.create(s -> {
            getExtendedServerResponse(request, s);
        });

        return obs;
    }
    
    /**
     * Queries the back end for a Compare API metrics.
     * @param request   the ServerRequest
     * @return  an Observable which can be subscribed to for comparison metrics
     */
    @SuppressWarnings("unchecked")
    public Observable<CompareResponse> getComparisonServerResponseObservable(CompareRequest request) {
        Observable<?> obs = 
            Observable.create(s -> getComparisonServerResponse(request, s))
                      .subscribeOn(Schedulers.from(ApplicationService.getInstance().getExecutorService()));

        return (Observable<CompareResponse>)obs;
    }
    
    /**
     * Queries the back end for a Compare API metrics.
     * @param request   the ServerRequest
     * @return  an Observable which can be subscribed to for comparison metrics
     */
    @SuppressWarnings("unchecked")
    public Observable<ServerResponse> getSimilarTermsServerResponseObservable(ServerRequest request) {
        Observable<?> obs = 
            Observable.create(s -> getSimilarTermsServerResponse(request, s))
                      .subscribeOn(Schedulers.from(ApplicationService.getInstance().getExecutorService()));

        return (Observable<ServerResponse>)obs;
    }
    
    /**
     * Queries the back end server for the detection of which language the supplied
     * text is in.
     * 
     * @param request       the {@link ServerRequest}
     * @return  an Observable which responds with the {@link FullClient} to use for queries 
     *          in a particular language.
     */
    @SuppressWarnings("unchecked")
    public Observable<ServerResponse> getLanguageDetectionObservable(ServerRequest request) {
        Observable<?> obs = 
            Observable.create(s -> getLanguageDetectionResponse(request, s))
                      .subscribeOn(Schedulers.from(ApplicationService.getInstance().getExecutorService()));
        
        return (Observable<ServerResponse>)obs;
    }

    //////////////////////////////////////////
    //         Internal Server Calls        //
    //////////////////////////////////////////
    /**
     * Calls the server and returns the {@link ServerResponse} via the specified
     * {@link Subscriber} on behalf of most of the tasks involving server lookups.
     * 
     * @param request       the {@link ServerRequest} object
     * @param s             the {@link Subscriber}
     */
    private void getServerResponse(ServerRequest request, Subscriber<? super ServerResponse> s) {
        (new Thread(() -> {
            threadedGetServerResponse(request, s);
        })).start();
    }
    
    /**
     * Defines action which retrieves query info on behalf of the InputWindow view tabs.
     * @param request
     * @param s
     */
    private void threadedGetServerResponse(final ServerRequest request, final Subscriber<? super ServerResponse> s) {
        try {
            ServerResponse response;
            FullClient client = request.getRetinaClient();
            
            switch(request.getInputViewType()) {
                case TOKEN: {
                    List<String> tokens = Arrays.asList(client.getTokensForText(request.getTermString()).get(0).split("[\\,]+"));
                    response = new ServerResponse(request, request.getInputViewType(), null, null, null, null, tokens, null, null, null);
                    if(response.getTokens().size() > 1) {
                        s.onError(new CompoundTermException(0, "Term: \"" + request.getTermString() + "\" is not a compound term!"));
                    }else{
                        s.onNext(response);
                    }
                    
                    break;
                }
                case TERM: {
                    List<Term> terms = null;
                    Term termLookup = null;
                    if((termLookup = lookup(request.getTermString())) != null) {
                        System.out.println("\n\n\n GOT TERM (" + request.getTermString().toUpperCase() + ") FROM CACHE !!!!!!!!!! \n\n\n");
                        terms = new ArrayList<>();
                        terms.add(termLookup);
                    }else{
                        terms = client.getTerms(request.getTermString(), 0, 10, true);
                    }

                    // Create copy due to need to mutate fields within the app.
                    List<Term> retVal = copyTerms(terms);

                    response = new ServerResponse(request, request.getInputViewType(), 
                        retVal, retVal.isEmpty() ? null : retVal.get(0).getFingerprint(), 
                            null, null, null, null, null, null);
                    s.onNext(response);

                    // Cache terms if not originally retrieved from cache.
                    if(termLookup == null) {
                        final List<Term> finalTerms = terms;
                        (new Thread(() -> {
                            storeTerms(finalTerms); 
                        })).start();
                    }

                    break;
                }
                case EXPRESSION: {
                    List<Context> contexts = null;
                    System.out.println("Request.lookupContexts() ? : " + request.lookupContexts());
                    if(request.lookupContexts()) {
                        try {
                            System.out.println("Querying Server for Contexts...");
                            contexts = client.getContextsForExpression(
                                request.getModel(), request.getContextsStartIndex(), 
                                request.getContextsMaxResults(), request.getSparsity(), true);
                            System.out.println("Got Contexts from Server: " + contexts);
                        }catch(Exception e) {
                            contexts = new ArrayList<>();
                        }
                    }

                    List<Term> terms = null;
                    try {
                        terms = client.getSimilarTermsForExpression(
                            request.getModel(), request.getSimTermsStartIndex(), request.getSimTermsMaxResults(), 
                            request.getContextID(), request.getPartOfSpeech(), true, request.getSparsity());
                    }catch(Exception e) {
                        terms = new ArrayList<>();
                    }

                    Fingerprint fp = client.getFingerprintForExpression(request.getModel(), request.getSparsity());
                    if(fp.getPositions() == null || fp.getPositions().length == 0) {
                        s.onError(new EmptyFingerprintException("\n\nThe specified expression resolved to an empty fingerprint."));
                        return;
                    }

                    Metric metric = null;
                    if(request instanceof CompareRequest && ((CompareRequest)request).getSecondaryModel() != null) {
                        client = request.getCompareClient();
                        metric = client.compare(request.getModel(), ((CompareRequest)request).getSecondaryModel());
                    }

                    response = new ServerResponse(request, request.getInputViewType(), terms, fp, contexts, null, null, null, null, metric);

                    s.onNext(response);

                    break;
                }
                case TEXT: {
                    Fingerprint fp = client.getFingerprintForText(request.getText());
                    List<String> keywords = client.getKeywordsForText(request.getText());
                    List<String> sentences = client.getTokensForText(request.getText());
                    List<String> tokens = request.getPosTags().isEmpty() ? 
                        sentences : client.getTokensForText(request.getText(), request.getPosTags());
                    List<Text> slices = client.getSlicesForText(request.getText(), request.getSlicesStartIndex(), 
                        request.getSlicesMaxResults(), false);

                    Metric metric = null;
                    if(request instanceof CompareRequest && ((CompareRequest)request).getSecondaryModel() != null) {
                        metric = client.compare(request.getModel(), ((CompareRequest)request).getSecondaryModel());
                    }

                    response = new ServerResponse(request, request.getInputViewType(), null, fp, null, keywords, tokens, slices, sentences, metric);

                    s.onNext(response);

                    break;
                }

                default: response = null;
            }
        }catch(Exception e) {
            // Pass the Exception to the Subscriber's handler
            s.onError(e);
            return;
        }

        s.onCompleted();
    }
    
    private class LanguageKey {
        private String language;
        int position;
        public LanguageKey(String l, int p) {
            this.language = l;
            this.position = p;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((language == null) ? 0 : language.hashCode());
            result = prime * result + position;
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            LanguageKey other = (LanguageKey) obj;
            if (language == null) {
                if (other.language != null)
                    return false;
            } else if (!language.equals(other.language))
                return false;
            if (position != other.position)
                return false;
            return true;
        }
    }

    /**
     * Calls the server and returns the {@link ServerResponse} via the specified
     * {@link Subscriber} for extended server calls such as tool tip similar term
     * lookups.
     * 
     * @param request       the {@link ServerRequest} object
     * @param s             the {@link Subscriber}
     */
    private void getExtendedServerResponse(ServerRequest request, Subscriber<? super ServerResponse> s) {
        try {
            ExtendedClient extendedClient = request.getExtendedClient();
            LanguageKey key = new LanguageKey(request.getRetinaLanguage(), request.getPosition());
            ServerResponse response;
            if(positionLookupCache.containsKey(key)) {
                response = positionLookupCache.get(key); 
                response.setCached(true);
            }else {
                List<Term> terms = extendedClient.getSimilarTermsForPosition(request.getPosition());
                response = new ServerResponse(request, null, terms, null, null, null, null, null, null, null);
                positionLookupCache.put(key, response);
            }

            s.onNext(response);
        }catch(Exception e) {
            if(s != null) {
                s.onError(e);
            }
        }
        if(s != null) {
            s.onCompleted();
        }
    }

    /**
     * Calls the server and returns the {@link ServerResponse} via the specified
     * {@link Subscriber} for comparison lookups.
     * 
     * @param request       the {@link ServerRequest} object
     * @param s             the {@link Subscriber}
     */
    private void getComparisonServerResponse(CompareRequest request, Subscriber<? super ServerResponse> s) {
        FullClient client = request.getCompareClient();
        Metric metric;
        try {
            metric = client.compare(request.getPrimaryModel(), request.getSecondaryModel());
            CompareResponse response = new CompareResponse(null, null, request, null, null, metric);
            s.onNext(response);
            s.onCompleted();
        }catch(Exception e) {
            if(s != null) {
                e.printStackTrace();
                s.onError(e);
            }
        }
    }
    
    /**
     * Calls the server and returns the {@link ServerResponse} via the specified
     * {@link Subscriber} for comparison lookups.
     * 
     * @param request       the {@link ServerRequest} object
     * @param s             the {@link Subscriber}
     */
    private void getSimilarTermsServerResponse(ServerRequest request, Subscriber<? super ServerResponse> s) {
        FullClient client = request.getRetinaClient();
        try {
            List<Term> similarTerms = client.getSimilarTermsForExpression(request.getModel(), 0, 10, -1, null, true, 1.0);
            ServerResponse response = new ServerResponse(request, null, similarTerms, null, null, null, null, null, null, null);
            s.onNext(response);
            s.onCompleted();
        }catch(Exception e) {
            if(s != null) {
                e.printStackTrace();
                s.onError(e);
            }
        }
    }
    
    private void getLanguageDetectionResponse(ServerRequest request, Subscriber<? super ServerResponse> s) {
        FullClient client = RetinaClientFactory.getClient(RetinaClientFactory.getRetinaTypes()[3]);
        try {
            Language l = client.getLanguageForText(request.getText());
            FullClient detectedClientType = RetinaClientFactory.getClient(l.getLanguage());
            ServerResponse r = new ServerResponse(request, null, null, null, null, null, null, null, null, null);
            r.setDetectedClient(detectedClientType);
            s.onNext(r);
            s.onCompleted();
        }catch(Exception e) {
            if(s != null) {
                e.printStackTrace();
                s.onError(e);
            }
        }
    }

    public ServerResponse lookupRequest(ServerRequest sr) {
        return requestCache.get(sr);
    }

    public ServerResponse lookupLastRequest(UUID id) {
        ServerRequest sr = sizeAndOrderTracker.reverseList().stream()
                .filter(r -> r.getWindow().getWindowID().equals(id))
                .findAny()
                .orElse(null);
        if(sr != null) {
            return requestCache.get(sr);
        }

        return null;
    }

    public void cacheRequest(ServerRequest req, ServerResponse resp) {
        if(requestCache.containsKey(req)) {
            return;
        }

        ServerRequest oldestReq = sizeAndOrderTracker.pushLast(req);
        if(oldestReq != null) {
            requestCache.remove(oldestReq);
        }
        requestCache.put(req, resp);
    }

    /**
     * Stores the terms in a cache
     * @param terms
     */
    void storeTerms(List<Term> terms) {
        for(Term t : terms) {
            if(t == null || 
                    t.getFingerprint() == null || 
                    termCache.containsKey(t.getTerm().toLowerCase())) 
                continue;

            // Store term in cache if non null
            termCache.put(t.getTerm().toLowerCase(), t);
        }
    }

    /**
     * Returns a boolean flag indicating whether the specified term 
     * is present in the cache or not.
     * @param termString
     * @return
     */
    boolean hasTerm(String termString) {
        return termCache.containsKey(termString.toLowerCase());
    }

    /**
     * Local {@link Term} lookup within the cache.
     * @param term
     * @return
     */
    public Term lookup(String term) {
        return termCache.get(term.toLowerCase());
    }

    /**
     * Checks the cache and if not present, does a server side lookup "reactively".
     * @param term
     * @param observer
     * @return
     */
//    public Observable<Term> lookup(String term, Observer<Term> observer) {
//        return Observable.create(sub -> {
//            (new Thread(() -> {
//                List<Term> terms = null;
//                Term termLookup = null;
//                if((termLookup = lookup(term)) != null) {
//                    terms = new ArrayList<>();
//                    terms.add(termLookup);
//                }else{
//                    try {
//                        terms = client.getTerms(term, 0, 10, true);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        observer.onError(e);
//                    }
//                }
//
//                // Create copy due to need to mutate fields within the app.
//                List<Term> retVal = copyTerms(terms);
//
//                // Cache terms if not originally retrieved from cache.
//                if(termLookup == null) {
//                    final List<Term> finalTerms = terms;
//                    (new Thread(() -> {
//                        storeTerms(finalTerms);
//                    })).start();
//                }
//
//                observer.onNext(retVal.get(0));
//                observer.onCompleted();
//            })).start();
//        });
//    }

    /**
     * Provides a reactive lookup of a term (either via cache or server lookup)
     * which is returned to the caller upon subscribing to the returned Observable.
     * The value is <em>not</em> returned on the calling thread so callers who are 
     * interacting with the UI must do so after queuing on the Platform UI thread.
     * 
     * @param requestingWindow      the Iris {@link Window} sourcing the request.
     * @param term
     * @return
     */
    public Observable<Term> lookupFn(Window requestingWindow, String term) {
        return Observable.create(sub -> {
            (new Thread(() -> {
                List<Term> terms = null;
                Term termLookup = null;
                if((termLookup = lookup(term)) != null) {
                    terms = new ArrayList<>();
                    terms.add(termLookup);
                }else{
                    try {
                        FullClient client = WindowService.getInstance().clientRetinaFor(requestingWindow);
                        terms = client.getTerms(term, 0, 10, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Create copy due to need to mutate fields within the app.
                List<Term> retVal = copyTerms(terms);

                // Cache terms if not originally retrieved from cache.
                if(termLookup == null) {
                    final List<Term> finalTerms = terms;
                    (new Thread(() -> {
                        storeTerms(finalTerms);
                    })).start();
                }

                sub.onNext(retVal.get(0));
                sub.onCompleted();

            })).start();
        });
    }

    /**
     * Returns a deep copy of the specified list of {@link Terms}
     * @param orig
     * @return
     */
    List<Term> copyTerms(List<Term> orig) {
        List<Term> retVal = new ArrayList<>();
        for(Term t : orig) {
            if(t == null) continue;
            Term atomic = new Term(t.getTerm());
            try {
                Field f = t.getClass().getDeclaredField("df");
                f.setAccessible(true);
                f.set(atomic, t.getDf());

                f = t.getClass().getDeclaredField("score");
                f.setAccessible(true);
                f.set(atomic, t.getScore());

                f = t.getClass().getDeclaredField("posTypes");
                f.setAccessible(true);
                f.set(atomic, t.getPosTypes());

                f = t.getClass().getDeclaredField("fingerprint");
                f.setAccessible(true);
                f.set(atomic, t.getFingerprint());
            }catch(Exception e) {
                e.printStackTrace();
            }

            retVal.add(atomic);
        }

        return retVal;
    }


}
