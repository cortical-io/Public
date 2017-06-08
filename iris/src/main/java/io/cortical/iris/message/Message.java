package io.cortical.iris.message;

import io.cortical.iris.view.ViewType;
import io.cortical.retina.model.Model;

/**
 * Encapsulates the message body and other information needed in 
 * order to successfully route this message.
 */
public class Message {
    private ViewType viewType;
    
    private Model message;
    
    public Message(ViewType vt, Model msg) {
        this.viewType = vt;
        this.message = msg;
    }
    
    /**
     * Returns the type of message this is, which in turn
     * determines the server-side endpoint called.
     * @return
     */
    public ViewType getType() {
        return viewType;
    }
    
    /**
     * Returns the message payload.
     * @return
     */
    public Model getMessage() {
        return message;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        try {
            result = prime * result + ((message == null) ? 0 : 
                message.toJson().hashCode());
        }catch(Exception e) { result = prime * result; }
        result = prime * result + ((viewType == null) ? 0 : viewType.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        Message other = (Message)obj;
        if(message == null) {
            if(other.message != null)
                return false;
        } else {
            try {
                if(!message.toJson().equals(other.message.toJson()))
                    return false;
            }catch(Exception e) { return false; }
        }
        if(viewType != other.viewType)
            return false;
        return true;
    }
}
