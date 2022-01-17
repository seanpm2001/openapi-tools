import ballerina/http;

listener http:Listener helloEp = new (9090);

service /payloadV on helloEp {
    resource function post pets(@http:Payload map<string> req) returns http:Ok {
        http:Ok ok = {body: ()};
        return ok;
    }

    resource function post pets02(@http:Payload{} map<string> req) returns http:Ok {
        http:Ok ok = {body: ()};
        return ok;
    }

    resource function post pets03(@http:Payload{mediaType: "application/x-www-form-urlencoded"} map<string> req) returns http:Ok {
        http:Ok ok = {body: ()};
        return ok;
    }
}
