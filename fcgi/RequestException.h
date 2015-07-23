/* 
 * File:   RequestException.h
 * Author: dudae
 *
 * Created on Nedeľa, 2015, júna 28, 14:16
 */

#ifndef REQUESTEXCEPTION_H
#define	REQUESTEXCEPTION_H

#include <exception>

class RequestException : public std::exception {
public:
    RequestException(std::string message) : message(message) {}
    
    ~RequestException() throw() {}
    
    virtual const char* what() const throw() {
        return message.c_str();
    }
    
private:
    std::string message;
};

#endif	/* REQUESTEXCEPTION_H */

