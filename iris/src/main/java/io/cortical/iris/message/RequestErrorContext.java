package io.cortical.iris.message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javafx.util.Pair;


/**
 * Container to hold items describing the context around a request error
 * for logging and reporting purposes.
 * 
 * @author cogmission
 */
@SuppressWarnings("serial")
public class RequestErrorContext extends Exception {
    private ServerRequest request;
    private ServerResponse response;
    private Throwable throwable;
    private UUID inputWindowUUID;
    
    private LocalDateTime timestamp;
    
    private boolean fromClipboard;
    
    /**
     * Creates a new {@code RequestErrorContext}.
     * @param req       the {@link ServerRequest} object.
     * @param resp      the {@link ServerResponse} object.
     * @param e         a throwable if appropriate.
     */
    public RequestErrorContext(ServerRequest req, ServerResponse resp, Throwable e) {
        this(req, resp, e, false);
    }
    
    /**
     * Creates a new {@code RequestErrorContext}.
     * @param req               the {@link ServerRequest} object.
     * @param resp              the {@link ServerResponse} object.
     * @param e                 a throwable if appropriate.
     * @param fromClipboard     flag indicating source of data is from the Clipboard.
     */
    @SuppressWarnings("unchecked")
    public RequestErrorContext(ServerRequest req, ServerResponse resp, Throwable e, boolean fromClipboard) {
        super(e);
        
        timestamp = LocalDateTime.now();
        this.inputWindowUUID = ((Pair<UUID, Message>)((Payload)req.getPayload()).getPayload()).getKey();
        this.request = req;
        this.response = resp;
        this.fromClipboard = fromClipboard;
    }
    
    /**
     * Returns the {@link UUID} of the {@link InputWindow}
     * @return	the {@link UUID} of the {@link InputWindow}
     */
    public UUID getInputWindowUUID() {
    	return inputWindowUUID;
    }

    /**
     * Returns the request object which preceded the error.
     * @return
     */
    public ServerRequest getRequest() {
        return request;
    }

    /**
     * Returns the {@link ServerResponse} (if it exists).
     * @return
     */
    public ServerResponse getResponse() {
        return response;
    }

    /**
     * Returns the {@link Throwable} associated with the error.
     * @return
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * Returns the timestamp
     * @return
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String getMessage() {
        String message = super.getMessage();
        if(message.indexOf("Exception:") != -1) {
            System.out.println("mess: " + message);
            message = message.substring(message.indexOf("Exception:") + "Exception:".length());
        }
        return message;
    }
    
    /**
     * Returns the flag indicating the source of this error's data
     * was from the clipboard.
     * @return
     */
    public boolean isFromClipboard() {
        return fromClipboard;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp.format(formatter));
        sb.append(" - ").append(getMessage());
        
        return sb.toString();
    }
    
}
