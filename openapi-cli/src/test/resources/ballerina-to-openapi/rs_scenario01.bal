import ballerina/http;

listener http:Listener helloEp = new (9090);

service /payloadV on helloEp {
    resource function get pets() returns http:Ok {
        http:Ok ok = {body: ()};
        return ok;
    }
}
