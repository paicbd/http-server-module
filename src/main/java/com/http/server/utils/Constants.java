package com.http.server.utils;

public class Constants {
    private Constants() {
        throw new IllegalStateException("Utility class");
    }

    public static final int IS_STARTED = 1;
	public static final String STARTED = "STARTED";
    public static final String STOPPED = "STOPPED";
    public static final String TYPE = "sp";
    public static final String WEBSOCKET_STATUS_ENDPOINT = "/app/handler-status";
    public static final String UPDATE_SERVICE_PROVIDER_ENDPOINT = "/app/http/updateServiceProvider";
    public static final String RESPONSE_HTTP_SERVER_ENDPOINT = "/app/response-http-server";
    public static final String UPDATE_SERVER_HANDLER_ENDPOINT = "/app/httpUpdateServerHandler";
    public static final String SERVICE_PROVIDER_DELETED_ENDPOINT = "/app/http/serviceProviderDeleted";
    public static final String PARAM_UPDATE_STATUS = "status";
    public static final String STOP_INSTANCE_ENDPOINT = "/app/stop-instance";
    public static final String GENERAL_SETTINGS_SMPP_HTTP_ENDPOINT = "/app/generalSettings";
    public static final int MAX_DESTINATIONS = 10000;
}
