package io.cortical.iris.ui.util;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import io.cortical.iris.ApplicationService;
import io.cortical.iris.CachingClientWrapper;
import io.cortical.iris.RetinaClientFactory;
import io.cortical.iris.ServerMessageService;
import io.cortical.iris.WindowService;
import io.cortical.iris.message.Message;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.message.ServerResponse;
import io.cortical.iris.view.ViewType;
import io.cortical.iris.window.InputWindow;
import io.cortical.iris.window.OutputWindow;
import io.cortical.iris.window.Window;
import io.cortical.retina.client.FullClient;
import io.cortical.retina.model.Term;
import javafx.util.Pair;
import rx.Observable;
import rx.Observer;

public class TermLookupAssistant {
    /**
     * Invokes stand alone server call to update the FingerprintDisplay with
     * a new Fingerprint representing the selected Term in this SimilarTermsDisplay.
     * 
     * @param term
     */
    public static void routeTermRequest(ServerRequest request, Consumer<Term> consumer) {
        ServerMessageService.getInstance().routeTermLookup(request, consumer);
    }
    
    public static void routeSimilarTermsRequest(Term term, Window w, Consumer<List<Term>> consumer) {
        FullClient reqClient = WindowService.getInstance().clientRetinaFor(w);
        ServerRequest requestPayload = new ServerRequest(0, 10, -1, null, null, term, 1.0, null);
        requestPayload.setRetinaClient(reqClient);
        
        CachingClientWrapper client = ApplicationService.getInstance().cachingClientProperty().get();
        Observable<ServerResponse> obs = client.getSimilarTermsServerResponseObservable((ServerRequest)requestPayload);
        Observer<ServerResponse> subscriber = new Observer<ServerResponse>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) {}
            @Override public void onNext(ServerResponse r) {
                consumer.accept(r.getTerms());
            }
        };
        obs.subscribe(subscriber);
    }
    
    /**
     * Prepares a one-off Term lookup request suitable for lookups in any Retina language.
     * 
     * <em>WARNING: EITHER THE INPUT OR THE OUTPUT WINDOW MUST BE SET, BOTH CANNOT BE NULL</em> 
     * 
     * @param term
     * @param iw
     * @param ow
     * @return      fully functional {@link ServerRequest}
     * @throws  IllegalArgumentException    if both the InputWindow and OutputWindow arguments
     *                                      are null.
     */
    public static ServerRequest prepareRequest(Term term, InputWindow iw, OutputWindow ow) {
        if(iw == null && ow == null) {
            throw new IllegalArgumentException("Both InputWindow and OutputWindow were null. One must be set...");
        }
        
        UUID inputId = null;
        if(iw == null) {
            inputId = ow.getTitleBar().getInputSelector().getPrimaryInputSelection();
            iw = (InputWindow)WindowService.getInstance().windowFor(inputId);
        } else {
            inputId = iw.getWindowID();
        } 
        
        ServerRequest req = new ServerRequest();
        req.setTermString(term.getTerm());
        req.setPayload(new Pair<UUID, Message>(inputId, new Message(ViewType.EXPRESSION, term)));
        req.setModel(term);
        req.setInputViewType(ViewType.TERM);
        
        FullClient currentClient = WindowService.getInstance().clientRetinaFor(iw);
        req.setRetinaClient(currentClient);
        
        String retinaName = RetinaClientFactory.getRetinaName(currentClient);
        req.setExtendedClient(RetinaClientFactory.getExtendedClient(retinaName));
        req.setRetinaLanguage(retinaName);
        
        return req;
    }
}
