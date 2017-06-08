package io.cortical.iris;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.bushe.swing.event.EventTopicSubscriber;

import io.cortical.iris.message.BusEvent;
import io.cortical.iris.message.CompareRequest;
import io.cortical.iris.message.CompareResponse;
import io.cortical.iris.message.EmptyFingerprintException;
import io.cortical.iris.message.EventBus;
import io.cortical.iris.message.Message;
import io.cortical.iris.message.Payload;
import io.cortical.iris.message.RequestErrorContext;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.message.ServerResponse;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.window.Window;
import io.cortical.retina.model.Model;
import io.cortical.retina.model.Term;
import io.cortical.util.Deque;
import io.cortical.util.ImmutableListCollector;
import javafx.application.Platform;
import javafx.util.Pair;
import rx.Observable;
import rx.Observer;
import rx.observables.BlockingObservable;


/**
 * Responsible for all messaging to the Retina API Service on behalf of the input and 
 * output windows.  
 * 
 * @author cogmission
 * 
 */
public class ServerMessageService {
    private static final ServerMessageService INSTANCE = new ServerMessageService();
    
    private static final int DEFAULT_CAPACITY = 100;
    private static final int DEFAULT_PER_WINDOW_CAPACITY = 10;
    
    /** Stores the last (most recent) query per InputWindow */
    private Map<UUID, Deque<ServerResponse>> windowResponseHistory = new HashMap<>();
    /** Stores the last (most recent) query per InputWindow */
    private Map<UUID, Deque<ServerRequest>> windowRequestHistory = new HashMap<>();
    /** General request history storage by {@link Window} */
    private Deque<ServerRequest> requestHistory = new Deque<>(DEFAULT_CAPACITY);
    /** General response history storage by {@link Window} */
    private Deque<ServerResponse> responseHistory = new Deque<>(DEFAULT_CAPACITY);
    
    
    
    /** Config item specifying wish to execute every query despite being equal to previous */
    private boolean isForcedMode;
    
    /**
     * Private singleton constructor
     */
    private ServerMessageService() {
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.SERVER_MESSAGE_REQUEST.subj() + "[\\w\\-]+"), getServerMessageHandler(false));
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.SERVER_MESSAGE_SECONDARY_REQUEST.subj() + "[\\w\\-]+"), getServerMessageHandler(true));
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.SERVER_MESSAGE_EXTERNAL_ROUTE_REQUEST.subj() + "[\\s\\w\\-]+"), getExternalRoutedMessageHandler());
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.FINGERPRINT_TOOLTIP_LOOKUP_REQUEST.subj() + "[\\w\\-]+"), getTooltipLookupHandler());
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.FINGERPRINT_DISPLAY_REPLACE.subj() + "[\\w\\-]+"), getColorIDReplaceHandler());
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.TERM_CACHE_LOOKUP_REQUEST.subj() + "[\\w\\-]+"), getTermCacheLookupHandler());
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.INPUT_EVENT_NAVIGATION_ACCEPTED.subj() + "[\\w\\-]+"), getQueueResetHandler());
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.SERVER_MESSAGE_EXECUTE_COMPARE_REQUEST.subj() + "[\\w\\-]+"), getCompareRequestHandler());
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.LANGUAGE_DETECTION_REQUEST.subj() + "[\\w\\-]+"), getLanguageDetectionRequestHandler());
        EventBus.get().subscribeTo(Pattern.compile(BusEvent.SERVER_MESSAGE_EXECUTE_SIMILARTERMS_REQUEST.subj() + "[\\w\\-]+"), getSimilarTermsRequestHandler());
    }
    
    /**
     * Returns the singleton instance of this {@code ServerMessageService}
     * @return
     */
    public static ServerMessageService getInstance() {
        return INSTANCE;
    }
    
    /**
     * Overrides redundancy check on queries if set to true.
     * @param b
     */
    public void setForcedMode(boolean b) {
        this.isForcedMode = b;
    }
    
    /**
     * Returns the flag indicating whether forced mode is "on".
     * 
     * @return the forced mode flag
     */
    public boolean isForcedMode() {
        return isForcedMode;
    }

    /**
     * Stores the response specified, keyed by the window specified.
     * @param r         the {@link ServerResponse} received from the server.
     * @param windowId  the id of the window for which the request is stored.
     */
    public void storeWindowResponse(ServerResponse r, UUID windowId) {
        if(r == null) return;
        
        Deque<ServerResponse> q = null;
        if((q = windowResponseHistory.get(windowId)) == null) {
            windowResponseHistory.put(windowId, q = new Deque<ServerResponse>(DEFAULT_PER_WINDOW_CAPACITY));
        }
        q.pushFirst(r);
        
        // Now store in the general request cache
        storeApplicationResponse(r);
    }
    
    /**
     * Returns the last response made on behalf of the {@link Window} specified
     * by the {@link UUID} passed.
     * @param windowId  the id of the window for which the last response is returned.
     * @return  the most recent {@link ServerResponse}
     */
    public ServerResponse getLastWindowResponse(UUID windowId) {
        Deque<ServerResponse> q = null;
        if((q = windowResponseHistory.get(windowId)) == null) {
            windowResponseHistory.put(windowId, q = new Deque<ServerResponse>(DEFAULT_PER_WINDOW_CAPACITY));
        }
        return q.peekFirst();
    }
    
    /**
     * Returns a flag indicating whether the response history
     * is empty or not.
     * @param windowId  the {@link Window} for which the history state is retrieved.
     * @return  true if empty, false if not.
     */
    public boolean hasCachedWindowResponse(UUID windowId) {
        return windowResponseHistory.containsKey(windowId) &&
            !windowResponseHistory.get(windowId).isEmpty();
    }
    
    /**
     * Returns a flag indicating whether the request history
     * is empty or not.
     * @param windowId  the {@link Window} for which the history state is retrieved.
     * @return  true if empty, false if not.
     */
    public boolean hasCachedWindowRequest(UUID windowId) {
        return windowRequestHistory.containsKey(windowId) &&
            !windowRequestHistory.get(windowId).isEmpty();
    }
    
    /**
     * Stores the Request specified, keyed by the window specified.
     * @param r         the {@link ServerRequest} received from the server.
     * @param windowId  the id of the window for which the request is stored.
     */
    public void storeWindowRequest(ServerRequest r, UUID windowId) {
        storeWindowRequest(r, windowId, true);
    }
    
    /**
     * Stores the Request specified, keyed by the window specified.
     * @param r                         the {@link ServerRequest} received from the server.
     * @param windowId                  the id of the window for which the request is stored.
     * @param storeApplicationScope     flag indicating that the request should also be stored
     *                                  in the application request cache.
     */
    public void storeWindowRequest(ServerRequest r, UUID windowId, boolean storeApplicationScope) {
        if(r == null) return;
        
        Deque<ServerRequest> q = null;
        if((q = windowRequestHistory.get(windowId)) == null) {
            windowRequestHistory.put(windowId, q = new Deque<ServerRequest>(DEFAULT_PER_WINDOW_CAPACITY));
        }
        q.pushFirst(r);
        
        // Now store in the general request cache
        if(storeApplicationScope) {
            storeApplicationRequest(r);
        }
    }
    
    /**
     * Returns the last request made on behalf of the {@link Window} specified
     * by the {@link UUID} passed.
     * @param windowId  the id of the window for which the last request is returned.
     * @return  the most recent {@link ServerRequest}
     */
    public ServerRequest getLastWindowRequest(UUID windowId) {
        Deque<ServerRequest> q = null;
        if((q = windowRequestHistory.get(windowId)) == null) {
            windowRequestHistory.put(windowId, q = new Deque<ServerRequest>(DEFAULT_PER_WINDOW_CAPACITY));
        }
        return q.peekFirst();
    }
    
    /**
     * Returns a flag indicating whether the request history
     * is empty or not.
     * @param windowId  the {@link Window} for which the history state is retrieved.
     * @return  true if empty, false if not.
     */
    public boolean hasLastWindowRequest(UUID windowId) {
        return windowRequestHistory.containsKey(windowId) &&
            !windowRequestHistory.get(windowId).isEmpty();
    }
    
    /**
     * Stores the specified {@link ServerRequest} in the request cache.
     * @param request
     */
    public void storeApplicationRequest(ServerRequest request) {
        if(request == null) return;
        requestHistory.pushFirst(request);
    }
    
    /**
     * Returns an unmodifiable list view of {@link ServerRequest}s.
     * @return  an unmodifiable list view of {@link ServerRequest}s.
     */
    public List<ServerRequest> readOnlyRequestHistory() {
        return StreamSupport.stream(requestHistory.spliterator(), false)
            .collect(ImmutableListCollector.toImmutableList());
    }
    
    /**
     * Stores the specified {@link ServerResponse} in the response cache.
     * @param response
     */
    public void storeApplicationResponse(ServerResponse response) {
        if(response == null) return;
        responseHistory.pushFirst(response);
    }
    
    /**
     * Returns an unmodifiable list view of {@link ServerResponse}s.
     * @return  an unmodifiable list view of {@link ServerResponse}s.
     */
    public List<ServerResponse> readOnlyResponseHistory() {
        return StreamSupport.stream(responseHistory.spliterator(), false)
            .collect(ImmutableListCollector.toImmutableList());
    }
    
    /**
     * Convenience method to assemble a Payload object properly.
     * @param w                 the Window for which the message is sent
     * @param routeForView      the view indicating which queries to execute
     * @param model             the source input
     * @return
     */
    public static Payload buildRequestPayload(Window w, ViewType routeForView, Model model) {
        Payload request = new Payload(new Pair<UUID, Message>(w.getWindowID(), new Message(ViewType.EXPRESSION, model)));
        request.setWindow(w);
        return request;
    }
    
    /**
     * Services one-off compound term lookup requests in the form of an attempt to "tokenize"
     * the specified term contained in the given {@link ServerRequest}. If the list tokens returned
     * are of size "1", then the specified term is a compound term (single entity). Otherwise, the
     * tokens returned reflect the fact that the term is actually many words and not a single word
     * (compound term).
     * 
     * @param req           the request sent to the server.
     * 
     * @return a list of tokens indicating whether the submitted term is compound or not.
     */
    public List<String> routeCompoundTermLookup(ServerRequest req) {
        ServerResponse response = null;
        
        try {
            CachingClientWrapper client = ApplicationService.getInstance().cachingClientProperty().get();
            Observable<ServerResponse> obs = client.getServerResponseObservable(req);
            BlockingObservable<ServerResponse> blocking = obs.toBlocking();
            response = blocking.single();
        }catch(Exception e) {
            RequestErrorContext rec = new RequestErrorContext(req, null, e);
            if(e.getClass().getSimpleName().indexOf("Timeout") != -1 ||
                e instanceof EmptyFingerprintException) {
                
                publishProblem(BusEvent.SERVER_MESSAGE_REQUEST_WARNING, rec);
            } else {
                publishProblem(BusEvent.SERVER_MESSAGE_REQUEST_ERROR, rec);
            }
        }
        
        return response.getTokens();
    }
    
    /**
     * Services one-off term lookup requests arising from bubble highlights and such...
     * 
     * @param req           the request sent to the server.
     * @param consumer      user submitted callback called with the resulting terms.
     */
    public void routeTermLookup(ServerRequest req, Consumer<Term> consumer) {
        CachingClientWrapper client = ApplicationService.getInstance().cachingClientProperty().get();
        // Check cache first
        if(client.hasTerm(req.getTermString())) {
            System.out.println("found in cache");
            consumer.accept(client.lookup(req.getTermString()));
            return;
        }
        
        // Send full request if not in cache
        Pattern subscriberPattern = Pattern.compile(BusEvent.SERVER_MESSAGE_EXTERNAL_ROUTE_RESPONSE.subj() + "simterm" + req.getTermString());
        EventTopicSubscriber<Payload> onSub = new EventTopicSubscriber<Payload>() {
            EventTopicSubscriber<Payload> sub;
            { // Use Instance initializer to obtain reference to the "this" of the anonymous class.
                sub = this;
            }
            public void onEvent(String topic, Payload p) {
                ServerResponse r = (ServerResponse)p;
                consumer.accept(r.getTerms().isEmpty() ? null : r.getTerms().get(0));
                
                EventBus.get().unsubscribeTo(subscriberPattern, sub);
            }
        };
        
        EventBus.get().subscribeTo(subscriberPattern, onSub);
        
        EventBus.get().broadcast(
            BusEvent.SERVER_MESSAGE_EXTERNAL_ROUTE_REQUEST.subj() + "simterm" + req.getTermString(), req);
    }
    
    /**
     * Called to forward the specified {@link ServerResponse} object to the recipient
     * window via the {@link EventBus}.
     * 
     * NOTE: This event is guaranteed to be delivered on the rendering thread.
     * 
     * @param busEvent      the channel on which to publish
     * @param r             the {@code ServerResponse} to publish
     * @param route         the identifier of the input or output used by channel listeners
     *                      to filter the input
     */
    private void publishResponse(BusEvent busEvent, ServerResponse r, UUID route) {
        Platform.runLater(() -> {
            storeWindowResponse(r, route);
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_HALT_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
            EventBus.get().broadcast(busEvent.subj() + route, r); 
        });
    }
    
    /**
     * Propagates the specified error to the application's event bus to be handled by the 
     * interested parties, down stream.
     * 
     * @param busEvent
     * @param errorContext
     */
    private void publishProblem(BusEvent busEvent, RequestErrorContext errorContext) {
    	Platform.runLater(() -> {
    		Payload p = new Payload();
    		p.setPayload(errorContext);
    		UUID route = errorContext.getInputWindowUUID();
    		EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_HALT_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
            EventBus.get().broadcast(busEvent.subj() + route, p);
        });
    }
    
    /**
     * Utility method to extract the UUID out of the specified {@link Payload}
     * object and return it.
     * @param p
     * @return
     */
    @SuppressWarnings("unchecked")
    private UUID getEmbeddedUUID(Payload p) {
        return ((Pair<UUID, Message>)((Payload)p.getPayload()).getPayload()).getKey();
    }
    
    /**
     * Returns the {@link EventTopicSubscriber} responsible for formulating server messages.
     * @return
     */
    private EventTopicSubscriber<Payload> getServerMessageHandler(final boolean isSecondary) {
        return (messageString, requestPayload) -> {
            UUID inputWindowUUID = getEmbeddedUUID(requestPayload);
        	EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_IN_PROGRESS.subj() + inputWindowUUID, Payload.DUMMY_PAYLOAD);
            
            try {
                CachingClientWrapper client = ApplicationService.getInstance().cachingClientProperty().get();
                Observable<ServerResponse> obs = client.getServerResponseObservable((ServerRequest)requestPayload);
                Observer<ServerResponse> subscriber = new Observer<ServerResponse>() {
                    @Override public void onNext(ServerResponse r) {
                        if(isSecondary) {
                            publishResponse(BusEvent.SERVER_MESSAGE_SECONDARY_RESPONSE, r, inputWindowUUID);
                        } else {
                            publishResponse(BusEvent.SERVER_MESSAGE_RESPONSE, r, inputWindowUUID);
                        }
                    }
                    @Override public void onCompleted() {}
                    @Override public void onError(Throwable e) {
                    	RequestErrorContext rec = new RequestErrorContext((ServerRequest)requestPayload, null, e);
                    	if(e.getClass().getSimpleName().indexOf("Timeout") != -1 ||
                            e instanceof EmptyFingerprintException) {
                    	    
                    	    publishProblem(BusEvent.SERVER_MESSAGE_REQUEST_WARNING, rec);
                    	} else {
                    	    publishProblem(BusEvent.SERVER_MESSAGE_REQUEST_ERROR, rec);
                    	}
                    }
                };
                
                obs.subscribe(subscriber);
            }catch(Exception e) {
                e.printStackTrace();
                EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_ERROR_PROGRESS.subj() + inputWindowUUID, Payload.DUMMY_PAYLOAD);
            }
        };
    }
    
    /**
     * Returns the {@link EventTopicSubscriber} responsible for one-off routed message handling.
     * 
     * Note: Route is a String used by the source sender such as "simterm" combined with the term to lookup
     * but could be anything distinctive to the function doing the lookup.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    private EventTopicSubscriber<Payload> getExternalRoutedMessageHandler() {
        return (messageString, requestPayload) -> {
            String route = messageString.substring(messageString.indexOf("_") + 1);
            UUID uuid = ((Pair<UUID, Message>)requestPayload.getPayload()).getKey();
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_IN_PROGRESS.subj() + uuid, Payload.DUMMY_PAYLOAD);
            
            try {
                CachingClientWrapper client = ApplicationService.getInstance().cachingClientProperty().get();
                Observable<ServerResponse> obs = client.getServerResponseObservable((ServerRequest)requestPayload);
                Observer<ServerResponse> subscriber = new Observer<ServerResponse>() {
                    @Override public void onNext(ServerResponse r) {
                        Platform.runLater(() -> {
                            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_HALT_PROGRESS.subj() + uuid, Payload.DUMMY_PAYLOAD);
                            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_EXTERNAL_ROUTE_RESPONSE.subj() + route, r); 
                        });
                    }
                    @Override public void onCompleted() {}
                    @Override public void onError(Throwable e) {
                        RequestErrorContext rec = new RequestErrorContext((ServerRequest)requestPayload, null, e);
                        // For now simply output
                        System.out.println(rec);
                    }
                };
                
                obs.subscribe(subscriber);
            }catch(Exception e) {
                e.printStackTrace();
                EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_ERROR_PROGRESS.subj() + uuid, Payload.DUMMY_PAYLOAD);
            }
        };
    }
        
    /**
     * Listener for extended tooltip for position lookups.
     * @return
     */
    private EventTopicSubscriber<Payload> getTooltipLookupHandler() {
        return (messageString, requestPayload) -> {
            String route = messageString.substring(messageString.indexOf("_") + 1);
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_IN_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
            
            try {
                CachingClientWrapper client = ApplicationService.getInstance().cachingClientProperty().get();
                Observable<ServerResponse> obs = client.getExtendedServerResponseObservable((ServerRequest)requestPayload);
                Observer<ServerResponse> subscriber = new Observer<ServerResponse>() {
                    @Override public void onNext(ServerResponse r) {
                        Platform.runLater(() -> {
                            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_HALT_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
                            EventBus.get().broadcast(BusEvent.FINGERPRINT_TOOLTIP_LOOKUP_RESPONSE.subj() + route, r); 
                        });
                    }
                    @Override public void onCompleted() {}
                    @Override public void onError(Throwable e) {
                        RequestErrorContext rec = new RequestErrorContext((ServerRequest)requestPayload, null, e);
                        // For now simply output
                        System.out.println(rec);
                    }
                };
                
                obs.subscribe(subscriber);
            }catch(Exception e) {
                e.printStackTrace();
                EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_ERROR_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
            }
        };
    }
    
    /**
     * Listener for inserting blanks into the queue after input tab changes, so that execution of a similar
     * query after returning to the to that tab - will still fire. Firing of queries depends on the requested
     * query being "different" from the last query; thus comparing with a "blank" will always allow duplicate
     * queries to execute, which is what we want after a reset.
     * 
     * @return  listener which handles insertion of a blank ServerRequest
     * @see ServerRequest#BLANK
     * 
     * IS THIS DEPRECATED (NEEDED) OR NOT?
     * @deprecated
     */
    private EventTopicSubscriber<Payload> getQueueResetHandler() {
        return (messageString, requestPayload) -> {
            UUID windowID = requestPayload.getWindow().getWindowID();
            storeWindowRequest(ServerRequest.BLANK, windowID, false);
        };
    }
    
    /**
     * Listener for re-sending the last query following a Color ID change in an InputWindow.
     * @return
     */
    private EventTopicSubscriber<Payload> getColorIDReplaceHandler() {
        return (messageString, requestPayload) -> {
            UUID windowID = requestPayload.getWindow().getWindowID();
            ServerRequest last = getLastWindowRequest(windowID);
            if(last != null && hasCachedWindowResponse(windowID)) {
                (new Thread(() -> {
                    EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_IN_PROGRESS.subj() + windowID, Payload.DUMMY_PAYLOAD);
                    
                    //Pause one second and a half so user can see progress when response is too quick.
                    try { Thread.sleep(1500); }catch(Exception e) { e.printStackTrace(); }
                    
                    EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_HALT_PROGRESS.subj() + windowID, Payload.DUMMY_PAYLOAD);
                    EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_RESPONSE.subj() + windowID, getLastWindowResponse(windowID));
                })).start();
            }
        };
    }
    
    /**
     * Handler which responds to cached term lookups occurring over the event bus.
     * @return
     */
    private EventTopicSubscriber<Payload> getTermCacheLookupHandler() {
        return (messageString, requestPayload) -> {
            CachingClientWrapper client = ApplicationService.getInstance().cachingClientProperty().get();
            String route = messageString.substring(messageString.indexOf("_") + 1);
            Term retVal = null;
            if((retVal = client.lookup((String)requestPayload.getPayload())) != null) {
                Payload p = new Payload(retVal);
                EventBus.get().broadcast(BusEvent.TERM_CACHE_LOOKUP_RESPONSE.subj() + route, p);
            }else{
                EventBus.get().broadcast(BusEvent.TERM_CACHE_LOOKUP_RESPONSE.subj() + route, Payload.DUMMY_PAYLOAD);
            }
        };
    }
    
    /**
     * Handler which listens for compare requests on the message bus and executes
     * the server query; then broadcasts the results.
     * @return  
     */
    private EventTopicSubscriber<Payload> getCompareRequestHandler() {
        return (messageString, requestPayload) -> {
            String route = messageString.substring(messageString.indexOf("_") + 1);
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_IN_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
            
            try {
                CachingClientWrapper client = ApplicationService.getInstance().cachingClientProperty().get();
                Observable<CompareResponse> obs = client.getComparisonServerResponseObservable((CompareRequest)requestPayload);
                Observer<CompareResponse> subscriber = new Observer<CompareResponse>() {
                    @Override public void onNext(CompareResponse r) {
                        Platform.runLater(() -> {
                            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_HALT_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
                            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_EXECUTE_COMPARE_RESPONSE.subj() + route, r);
                        });
                    }
                    @Override public void onCompleted() {}
                    @Override public void onError(Throwable e) {
                        RequestErrorContext rec = new RequestErrorContext((ServerRequest)requestPayload, null, e);
                        // For now simply output
                        System.out.println(rec);
                    }
                };
                
                obs.subscribe(subscriber);
            }catch(Exception e) {
                e.printStackTrace();
                EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_ERROR_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
            }
        };
    }  
    
    /**
     * Handler which listens for compare requests on the message bus and executes
     * the server query; then broadcasts the results.
     * @return  
     */
    private EventTopicSubscriber<Payload> getSimilarTermsRequestHandler() {
        return (messageString, requestPayload) -> {
            String route = messageString.substring(messageString.indexOf("_") + 1);
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_IN_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
            
            try {
                CachingClientWrapper client = ApplicationService.getInstance().cachingClientProperty().get();
                Observable<ServerResponse> obs = client.getSimilarTermsServerResponseObservable((ServerRequest)requestPayload);
                Observer<ServerResponse> subscriber = new Observer<ServerResponse>() {
                    @Override public void onNext(ServerResponse r) {
                        Platform.runLater(() -> {
                            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_HALT_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
                            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_EXECUTE_SIMILARTERMS_RESPONSE.subj() + route, r);
                        });
                    }
                    @Override public void onCompleted() {}
                    @Override public void onError(Throwable e) {
                        RequestErrorContext rec = new RequestErrorContext((ServerRequest)requestPayload, null, e);
                        // For now simply output
                        System.out.println(rec);
                    }
                };
                
                obs.subscribe(subscriber);
            }catch(Exception e) {
                e.printStackTrace();
                EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_ERROR_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
            }
        };
    }
    
    /**
     * Handler which listens for language detection requests; executes the server query then broadcasts
     * the results.
     * 
     * @return
     */
    private EventTopicSubscriber<Payload> getLanguageDetectionRequestHandler() {
        return (messageString, requestPayload) -> {
            String route = messageString.substring(messageString.indexOf("_") + 1);
            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_IN_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
            
            try {
                CachingClientWrapper client = ApplicationService.getInstance().cachingClientProperty().get();
                Observable<ServerResponse> obs = client.getLanguageDetectionObservable((ServerRequest)requestPayload);
                Observer<ServerResponse> subscriber = new Observer<ServerResponse>() {
                    @Override public void onNext(ServerResponse r) {
                        Platform.runLater(() -> {
                            EventBus.get().broadcast(BusEvent.SERVER_MESSAGE_HALT_PROGRESS.subj() + route, Payload.DUMMY_PAYLOAD);
                            EventBus.get().broadcast(BusEvent.LANGUAGE_DETECTION_RESPONSE.subj() + route, r);
                        });
                    }
                    @Override public void onCompleted() {}
                    @Override public void onError(Throwable e) {
                        RequestErrorContext rec = new RequestErrorContext((ServerRequest)requestPayload, null, e);
                        // For now simply output
                        System.out.println(rec);
                    }
                };
                
                obs.subscribe(subscriber);
            }catch(Exception e) {
                
            }
        };
    }
}
