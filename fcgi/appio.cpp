#include "appio.h"

#include <Poco/URI.h>
#include <Poco/JSON/Array.h>
#include <Poco/JSON/Parser.h>
#include <Poco/JSON/Stringifier.h>
#include <Poco/RegularExpression.h>

#include "RequestException.h"
#include "RecordManager.h"

using namespace std;
using namespace Poco::JSON;
using namespace Poco::Dynamic;

Poco::RegularExpression action_re("\\/v\\d+\\/([^\\/\\?]+)\\/?\\??.*$");

const unsigned long STDIN_MAX = 2147483648;

string get_request_content(const FCGX_Request & request) {
    char * content_length_str = FCGX_GetParam("CONTENT_LENGTH", request.envp);
    unsigned long content_length = STDIN_MAX;

    if (content_length_str) {
        content_length = strtol(content_length_str, &content_length_str, 10);
        if (*content_length_str) {
            cerr << "Can't Parse 'CONTENT_LENGTH='"
                 << FCGX_GetParam("CONTENT_LENGTH", request.envp)
                 << "'. Consuming stdin up to " << STDIN_MAX << endl;
        }

        if (content_length > STDIN_MAX) {
            content_length = STDIN_MAX;
        }
    } else {
        // Do not read from stdin if CONTENT_LENGTH is missing
        content_length = 0;
    }

    char * content_buffer = new char[content_length];
    cin.read(content_buffer, content_length);
    content_length = cin.gcount();

    // Chew up any remaining stdin - this shouldn't be necessary
    // but is because mod_fastcgi doesn't handle it correctly.

    // ignore() doesn't set the eof bit in some versions of glibc++
    // so use gcount() instead of eof()...
    do cin.ignore(1024); while (cin.gcount() == 1024);

    string content(content_buffer, content_length);
    delete [] content_buffer;
    return content;
}

void print_header() {
    cout << "Content-type: application/json\r\n";
    cout << "Access-Control-Allow-Origin: *\r\n";
    cout << "\r\n";
}

void appio::print_ok(const string& message) {
    print_header();
    Object root;
    root.set("status", "ok");
    root.set("message", message);
    
    Stringifier::stringify(root, cout);
}

void appio::print_ok(const SearchResult& searchResult) {
    print_header();
    
    Record record = RecordManager::getInstance().getRecord(searchResult.getId());
    
    Object data;
    data.set("found", searchResult.isFound());
    data.set("distance", searchResult.getDistance());
    data.set("record", record.toJSON());
    
    Object root;
    root.set("status", "ok");
    root.set("data", data);
    
    Stringifier::stringify(root, cout);
}

void appio::print_ok(const std::vector<SearchResult>& searchResult) {
    print_header();
    
    Poco::JSON::Array list;
    
    for (vector<SearchResult>::const_iterator it = searchResult.begin(); it != searchResult.end(); it++) {
        Record record = RecordManager::getInstance().getRecord(it->getId());
        Object data;
        data.set("distance", it->getDistance());
        data.set("record", record.toJSON());
        list.add(data);
    }
    Object root;
    root.set("status", "ok");
    root.set("data", list);
    
    Stringifier::stringify(root, cout);
}

void appio::print_error(const string& message) {
    print_header();
    Object root;
    root.set("status", "error");
    root.set("message", message);
    
    Stringifier::stringify(root, cout);
}

void appio::print_error(const char* message) {
    print_header();
    Object root;
    root.set("status", "error");
    root.set("message", message);
    
    Stringifier::stringify(root, cout);
}

std::string appio::get_request_action(FCGX_Request request) {
    string request_uri = FCGX_GetParam("REQUEST_URI", request.envp);
    Poco::RegularExpression::MatchVec matches;
    action_re.match(request_uri, 0, matches);
    
    if (matches.size() > 1) {
        Poco::RegularExpression::Match match = matches[1];
        return request_uri.substr(match.offset, match.length);
    } else {
        throw RequestException("Invalid uri.");
    }
}

Poco::Dynamic::Var appio::get_request_json(FCGX_Request request) {
    try {
        Parser parser;
        string request_content = get_request_content(request);
        if (request_content.empty()) {
            return Poco::Dynamic::Var();
        } else {
            return parser.parse(request_content);
        }
    } catch (JSONException& e) {
        throw RequestException("Error occured during parsing JSON.");
    }
}

map<string, string> appio::get_request_params(FCGX_Request request) {
    string request_uri = FCGX_GetParam("REQUEST_URI", request.envp);
    map<string, string> result;
    
    Poco::URI uri(request_uri);
    Poco::URI::QueryParameters params = uri.getQueryParameters();
    
    for (Poco::URI::QueryParameters::const_iterator it = params.begin(); it != params.end(); it++) {
        result[it->first] = it->second;
    }
    
    return result;
}