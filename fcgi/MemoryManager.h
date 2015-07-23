/* 
 * File:   MemoryManager.h
 * Author: dudae
 *
 * Created on Štvrtok, 2015, júla 9, 12:03
 */

#ifndef MEMORYMANAGER_H
#define	MEMORYMANAGER_H

#include <stdint.h>
#include <map>
#include <string>

#include <Poco/SharedMemory.h>
#include <Poco/NamedMutex.h>
#include <Poco/NamedEvent.h>
#include <Poco/File.h>

#include "HashType.h"

class MemoryManager {
public:
    static MemoryManager& getInstance();
    MemoryManager(const MemoryManager& copy);
    virtual ~MemoryManager();
    
    void lockRead();
    void unlockRead();
    
    void lockWrite();
    void unlockWrite();
    
    const char* getData(HashType hashType);
    void releaseData(HashType hashType);
    uint64_t getDataSize(HashType hashType);
    void setData(HashType hashType, const char* data, size_t size);
private:
    MemoryManager();
    
    std::map<HashType, Poco::File*> mem_fds;
    std::map<HashType, Poco::SharedMemory*> mem_ptrs;
    
    Poco::File* readers_count_fd;
    Poco::File* can_read_fd;
    
    Poco::SharedMemory* readers_count;
    Poco::SharedMemory* can_read;
    
    Poco::NamedMutex* readers_count_lock;
    Poco::NamedMutex* can_read_lock;
};

#endif	/* MEMORYMANAGER_H */

