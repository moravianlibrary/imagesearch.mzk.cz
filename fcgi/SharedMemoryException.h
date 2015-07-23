/* 
 * File:   SharedMemoryException.h
 * Author: dudae
 *
 * Created on Štvrtok, 2015, júla 9, 13:31
 */

#ifndef SHAREDMEMORYEXCEPTION_H
#define	SHAREDMEMORYEXCEPTION_H

#include <exception>

class SharedMemoryException : public std::exception {
public:
    SharedMemoryException(std::string message) : message(message) {}
    
    ~SharedMemoryException() throw() {}
    
    virtual const char* what() const throw() {
        return message.c_str();
    }
    
private:
    std::string message;
};

#endif	/* SHAREDMEMORYEXCEPTION_H */

