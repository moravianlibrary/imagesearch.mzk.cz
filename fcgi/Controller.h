/* 
 * File:   Controller.h
 * Author: dudae
 *
 * Created on Nedeľa, 2015, júna 28, 14:30
 */

#ifndef CONTROLLER_H
#define	CONTROLLER_H

#include <string>
#include <map>

#include <Poco/Dynamic/Var.h>

#include "Record.h"

class Controller {
public:
    Controller();
    
    virtual ~Controller();
    
    void ingestRequest(const Poco::Dynamic::Var& request);
    
    void commitRequest();
    
    void searchIdenticalRequest(std::map<std::string, std::string>& params, const Poco::Dynamic::Var& request);
    
    void searchSimilarRequest(std::map<std::string, std::string>& params, const Poco::Dynamic::Var& request);
    
protected:
    
    void putRequest(const Record& record);
    
    void deleteRequest(const Record& record);
    
private:
    
    static int char2bin(char c);
    
    static std::vector<uint64_t> str2bin(const std::string& str);
    
    
};

#endif	/* CONTROLLER_H */

