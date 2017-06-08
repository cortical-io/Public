package io.cortical.iris.message;

import io.cortical.iris.ServerMessageService;
import io.cortical.iris.window.OutputWindow;

/**
 * Defines the subscribable events present in the app.
 * 
 * @author cogmission
 * @see EventBus
 * @see Payload
 */
public enum BusEvent {
    
    //////////////////////////////////////
    //   Window Request Event Types     //
    //////////////////////////////////////
    WINDOW_CLOSE_REQUEST("WindowCloseRequest"),
    WINDOW_HIDE_REQUEST("WindowHideRequest"),
    WINDOW_ICONIZE_REQUEST("WindowIconizeRequest"),
    WINDOW_DE_ICONIZE_REQUEST("WindowDe-IconizeRequest"),
    
    //////////////////////////////////////
    //   Show Modal Dialog Above App    //
    //////////////////////////////////////
    OVERLAY_SHOW_MODAL_DIALOG("OverlayShowModalDialog"),
    OVERLAY_DISMISS_MODAL_DIALOG("OverlayDismissModalDialog"),
    
    /////////////////////////////////////
    //  Notify OutputWindows of        //
    //  InputWindow's InputType change //
    /////////////////////////////////////
    INPUT_EVENT_NAVIGATION_ACCEPTED("InputEventNavAccepted_"),
    
    ////////////////////////////////////////
    //   Notify Input cleared from view   //
    ////////////////////////////////////////
    INPUT_EVENT_INPUT_CLEARED("InputEventInputCleared"),
    
    /** 
     * All InputViewAreas will broadcast the state of their views when
     * receiving this request.
     */
    INPUT_VIEWTYPE_CHANGE_BROADCAST_REQUEST("InputViewTypeBroadcastRequest"),
    /** Notifies interested parties (connected {@link OutputWindow}s) when input is changed */
    INPUT_EVENT_NEW_EXPRESSION_STATE("InputEventDirty_"),
    
    //////////////////////////////////////
    //      Server Message Handling     //
    //////////////////////////////////////
    /** Event sent by invoker of server message */
    SERVER_MESSAGE_IN_PROGRESS("ServerMessageInProgress_"),
    /** Event sent by invoker of server message TO HALT */
    SERVER_MESSAGE_HALT_PROGRESS("ServerMessageHaltProgress_"),
    /** Event sent by invoker of server message ON ERROR */
    SERVER_MESSAGE_ERROR_PROGRESS("ServerMessageErrorProgress_"),
    /** Event sent by invoker of server message RESET STATE */
    SERVER_MESSAGE_RESET_PROGRESS("ServerMessageResetProgress_"),
    /** Event sent by input windows having created text body of request */
    SERVER_MESSAGE_REQUEST_CREATED("ServerMessageCreated_"),
    /** Event sent by output windows which contains body and possible request parameters */
    SERVER_MESSAGE_REQUEST("ServerMessageRequest_"),
    /** Event sent by output windows which contains body and possible request parameters for secondary requests */
    SERVER_MESSAGE_SECONDARY_REQUEST("ServerMessageSecondaryRequest_"),
    /** Event sent by the ServerMessageService and received by OutputWindows */
    SERVER_MESSAGE_RESPONSE("ServerMessageResponse_"),
    /** Event sent by the ServerMessageService and received by OutputWindows for secondary responses */
    SERVER_MESSAGE_SECONDARY_RESPONSE("ServerMessageSecondaryResponse_"),
    /** Event sent by OutputWindows which newly connect to InputWindows */
    SERVER_MESSAGE_RESEND_RESPONSE_REQUEST("ServerMessageResendResponse_"),
    /** Event sent when an InputWindow's input is cleared or marked invalid */
    SERVER_MESSAGE_RESET("ServerMessageReset_"),
    /** Event request that is specifically routed on behalf of the sender */
    SERVER_MESSAGE_EXTERNAL_ROUTE_REQUEST("ServerMessageExternalRouteRequest_"),
    /** Event response that is specifically returned to the sender */
    SERVER_MESSAGE_EXTERNAL_ROUTE_RESPONSE("ServerMessageExternalRouteResponse_"),
    /** Event requested by InputWindow View such as an ID Color change */
    SERVER_MESSAGE_RELOAD_CACHED_RESPONSE("ServerMessageReloadCachedResponse_"),
    /** Used to indicate to an OutputWindow to manually send the last query */ 
    SERVER_MESSAGE_RE_EXECUTE_LAST_QUERY("ServerMessageExecuteLastQuery_"),
    /** Used to request an input window send any model message it currently has */
    SERVER_MESSAGE_SEND_CURRENT_MODEL_QUERY("ServerMessageSendCurrentModelQuery_"),
    /** Event sent by the OutputViewArea to request a compare server request */
    SERVER_MESSAGE_EXECUTE_COMPARE_REQUEST("ServerMessageExecuteCompareRequest_"),
    /** Event returned by the ServerMessageService on behalf of a given compare request */
    SERVER_MESSAGE_EXECUTE_COMPARE_RESPONSE("ServerMessageExecuteCompareResponse_"),
    /** Event sent by the ContextDisplay for similar terms */
    SERVER_MESSAGE_EXECUTE_SIMILARTERMS_REQUEST("ServerMessageExecuteSimilarTermsRequest_"),
    /** Event returned by the ServerMessageService on behalf of similar terms server request */
    SERVER_MESSAGE_EXECUTE_SIMILARTERMS_RESPONSE("ServerMessageExecuteSimilarTermsResponse_"),
    /** Event sent by the {@link ServerMessageService} to indicate an error with the last request */
    SERVER_MESSAGE_REQUEST_ERROR("ServerMessageRequestError_"),
    /** Event sent when a non-user error has occurred such as a timeout or a choice has to be made by the user. */
    SERVER_MESSAGE_REQUEST_WARNING("ServerMessageRequestWarning_"),
    
    ///////////////////////////////////////////
    //       General User Prompt             //
    ///////////////////////////////////////////
    APPLICATION_WINDOW_MESSAGE_PROMPT("ApplicationWindowMessagePrompt_"),
    
    ///////////////////////////////////////////
    //         Save Dialog Prompt            //
    ///////////////////////////////////////////
    APPLICATION_WINDOW_SAVE_PROMPT("ApplicationWindowSavePrompt_"),
    
    ///////////////////////////////////////////
    //         Load Dialog Prompt            //
    ///////////////////////////////////////////
    APPLICATION_WINDOW_LOAD_PROMPT("ApplicationWindowLoadPrompt_"),
    
    ///////////////////////////////////////////
    //    Output Config Map Deletion Helper  //
    ///////////////////////////////////////////
    APPLICATION_WINDOW_DELETE_PROMPT("ApplicationWindowDeletePrompt"),
    
    ///////////////////////////////////////////
    //   Input Connection to Output Window   //
    //   Needed to determine response        //
    //   resend need.                        //
    ///////////////////////////////////////////
    INPUT_WINDOW_SELECTED_EVENT("InputWindowSelectedEvent"),
    
    ///////////////////////////////////////////
    //      Fingerprint Display Updates      //
    ///////////////////////////////////////////
    FINGERPRINT_DISPLAY_ADD("FingerprintDisplayAdd_"),
    FINGERPRINT_DISPLAY_REMOVE("FingerprintDisplayRemove_"),
    FINGERPRINT_DISPLAY_REPLACE("FingerprintDisplayReplace_"),
    FINGERPRINT_DISPLAY_REMOVE_BY_COLOR("FingerprintDisplayRemoveByColor_"),
    
    ///////////////////////////////////////////
    //    Fingerprint Tooltip Similar Terms  //
    ///////////////////////////////////////////
    FINGERPRINT_TOOLTIP_LOOKUP_REQUEST("FingerprintTooltipLookupRequest_"),
    FINGERPRINT_TOOLTIP_LOOKUP_RESPONSE("FingerprintTooltipLookupResponse_"),
    
    ///////////////////////////////////////////
    //    Language Detection Request         //
    ///////////////////////////////////////////
    LANGUAGE_DETECTION_REQUEST("LanguageDetectionRequest_"),
    LANGUAGE_DETECTION_RESPONSE("LanguageDetectionResponse_"),
    
    ///////////////////////////////////////////
    //  Reset Connected OutputWindow Views   //
    ///////////////////////////////////////////
    RESET_CONNECTED_OUTPUT_VIEW_REQUEST("ResetConnectedOutputViewRequest_"),
    
    ///////////////////////////////////////////
    //          Term Cache Lookup            //
    ///////////////////////////////////////////
    TERM_CACHE_LOOKUP_REQUEST("TermCacheLookupRequest_"),
    TERM_CACHE_LOOKUP_RESPONSE("TermCacheLookupResponse_");
    
    private String subject;
    
    private BusEvent(String s) {
        this.subject = s;
    }
    
    /**
     * Returns the string form of the enum.
     * @return  the string form of the enum.
     */
    public String subj() {
        return subject;
    }
}
