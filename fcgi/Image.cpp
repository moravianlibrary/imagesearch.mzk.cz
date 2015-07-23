/* 
 * File:   Image.cpp
 * Author: dudae
 * 
 * Created on Streda, 2015, júla 8, 8:33
 */

#include "Image.h"
#include "RequestException.h"

#include <sstream>
#include <Poco/Base64Decoder.h>
#include <Poco/URI.h>
#include <Poco/Net/HTTPClientSession.h>
#include <Poco/Net/HTTPRequest.h>
#include <Poco/Net/HTTPResponse.h>

using namespace std;

using Poco::Net::HTTPClientSession;
using Poco::Net::HTTPRequest;
using Poco::Net::HTTPResponse;

std::vector<uint8_t>* Image::fromUrl(const std::string& url) {
    Poco::URI uri = Poco::URI(url);
    string path = uri.getPathAndQuery();
    if (path.empty()) path = "/";
    
    HTTPClientSession session(uri.getHost(), uri.getPort());
    HTTPRequest request(HTTPRequest::HTTP_GET, path);
    HTTPResponse response;
    
    session.sendRequest(request);
    istream& rs = session.receiveResponse(response);
    
    if (response.getStatus() == HTTPResponse::HTTP_OK) {
        vector<uint8_t>* result = new vector<uint8_t>;
        result->insert(result->begin(), istreambuf_iterator<char>(rs), istreambuf_iterator<char>());
        return result;
    } else {
        ostringstream os;
        os << "Unable to retrieve image from url: " << url << ". Returned status code: " << (int) response.getStatus();
        throw RequestException(os.str());
    }
}
    
std::vector<uint8_t>* Image::fromBase64(const std::string& base64) {
    vector<uint8_t>* result = new vector<uint8_t>;
    istringstream is(base64);
    Poco::Base64Decoder b64in(is);
    
    result->insert(result->begin(), istreambuf_iterator<char>(b64in), istreambuf_iterator<char>());
    return result;
}

