/* 
 * File:   LogicalException.h
 * Author: dudae
 *
 * Created on Streda, 2015, júla 22, 9:46
 */

#ifndef LOGICALEXCEPTION_H
#define	LOGICALEXCEPTION_H

#include <exception>

class LogicalException : public std::exception {
public:
    LogicalException(std::string message) : message(message) {}
    
    ~LogicalException() throw() {}
    
    virtual const char* what() const throw() {
        return message.c_str();
    }
    
private:
    std::string message;
};

#endif	/* LOGICALEXCEPTION_H */

