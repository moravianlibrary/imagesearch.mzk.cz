/* 
 * File:   DHashException.h
 * Author: dudae
 *
 * Created on Streda, 2015, júla 22, 16:54
 */

#ifndef DHASHEXCEPTION_H
#define	DHASHEXCEPTION_H

class DHashException : public std::exception {
public:
    DHashException(std::string message) : message(message) {}
    
    ~DHashException() throw() {}
    
    virtual const char* what() const throw() {
        return message.c_str();
    }
    
private:
    std::string message;
};

#endif	/* DHASHEXCEPTION_H */

