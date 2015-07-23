/* 
 * File:   main.cpp
 * Author: dudae
 *
 * Created on Nedeľa, 2015, júna 28, 9:30
 */

#include <cstdlib>
#include <iostream>
#include <string>
#include <exception>
#include <fcgio.h>

#include <Poco/Exception.h>
#include <dhash.h>

#include "appio.h"
#include "Controller.h"
#include "RequestException.h"
#include "RecordManager.h"
#include "HashManager.h"

using namespace std;

/*
 * 
 */
int main(int argc, char** argv) {
    FCGX_Request request;
    
    FCGX_Init();
    FCGX_InitRequest(&request, 0, 0);
    
    dhash_init();
    
    Controller controller;

    // It connects to a database
    RecordManager::getInstance();
    
    while (FCGX_Accept_r(&request) == 0) {
        fcgi_streambuf cin_fcgi_streambuf(request.in);
        fcgi_streambuf cout_fcgi_streambuf(request.out);
        fcgi_streambuf cerr_fcgi_streambuf(request.err);
        
        cin.rdbuf(&cin_fcgi_streambuf);
        cout.rdbuf(&cout_fcgi_streambuf);
        cerr.rdbuf(&cerr_fcgi_streambuf);
        
        try {
            string action = appio::get_request_action(request);
            
            if (action == "ingest") {
                controller.ingestRequest(appio::get_request_json(request));
            } else if (action == "commit") {
                controller.commitRequest();
            } else if (action == "searchIdentical") {
                map<string, string> params = appio::get_request_params(request);
                controller.searchIdenticalRequest(params, appio::get_request_json(request));
            } else if (action == "searchSimilar") {
                map<string, string> params = appio::get_request_params(request);
                controller.searchSimilarRequest(params, appio::get_request_json(request));
            } else {
                throw RequestException("Invalid url.");
            }
        } catch (Poco::Exception& e) {
            appio::print_error(e.displayText());
        } catch (exception& e) {
            appio::print_error(e.what());
        }
    }
    
    dhash_final();
    
    return 0;
}
