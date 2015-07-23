/* 
 * File:   MemoryManager.cpp
 * Author: dudae
 * 
 * Created on Štvrtok, 2015, júla 9, 12:03
 */

#include <stdint.h>
#include <unistd.h>
#include <cstring>
#include <iostream>

#include "MemoryManager.h"
#include "SharedMemoryException.h"

using namespace std;

const string READERS_COUNT_LOCK = "IMAGESEARCH.MZK.CZ.READERS_COUNT_LOCK";
const string CAN_READ_LOCK = "IMAGESEARCH.MZK.CZ.CAN_READ_LOCK";

MemoryManager& MemoryManager::getInstance() {
    static MemoryManager instance;
    return instance;
}

MemoryManager::MemoryManager() {
    mem_fds[BlockHash] = new Poco::File("/shared/blockhash");
    mem_fds[DHash] = new Poco::File("/shared/dhash");
    mem_fds[CannyDHash] = new Poco::File("/shared/cannydhash");
    
    readers_count_fd = new Poco::File("/shared/readers_count");
    can_read_fd = new Poco::File("/shared/can_read");
            
    readers_count = new Poco::SharedMemory(*readers_count_fd, Poco::SharedMemory::AM_WRITE);
    can_read = new Poco::SharedMemory(*can_read_fd, Poco::SharedMemory::AM_WRITE);
    readers_count_lock = new Poco::NamedMutex(READERS_COUNT_LOCK);
    can_read_lock = new Poco::NamedMutex(CAN_READ_LOCK);
}

MemoryManager::MemoryManager(const MemoryManager& copy) {
    
}

MemoryManager::~MemoryManager() {
    for (map<HashType, Poco::File*>::iterator it = mem_fds.begin(); it != mem_fds.end(); it++) {
        delete it->second;
    }
    
    delete readers_count_fd;
    delete can_read_fd;
    
    delete readers_count;
    delete can_read;
    delete readers_count_lock;
    delete can_read_lock;
}

void MemoryManager::lockRead() {
    while (true) {
        can_read_lock->lock();
        uint8_t* can_read_p = (uint8_t*) can_read->begin();
        if (*can_read_p) {
            readers_count_lock->lock();
            uint64_t* readers_count_p = (uint64_t*) readers_count->begin();
            *readers_count_p = *readers_count_p + 1;
            readers_count_lock->unlock();
            can_read_lock->unlock();
            break;
        } else {
            can_read_lock->unlock();
            usleep(500);
        }
    }
}

void MemoryManager::unlockRead() {
    readers_count_lock->lock();
    uint64_t* readers_count_p = (uint64_t*) readers_count->begin();
    if (*readers_count_p == 0) {
        readers_count_lock->unlock();
        throw SharedMemoryException("There is no read lock.");
    }
    *readers_count_p = *readers_count_p - 1;
    readers_count_lock->unlock();
}

void MemoryManager::lockWrite() {
    can_read_lock->lock();
    uint8_t* can_read_p = (uint8_t*) can_read->begin();
    if (!*can_read_p) {
        can_read_lock->unlock();
        throw SharedMemoryException("Update process is running.");
    }
    *can_read_p = 0;
    can_read_lock->unlock();
    
    while (true) {
        readers_count_lock->lock();
        uint64_t* readers_count_p = (uint64_t*) readers_count->begin();
        if (*readers_count_p == 0) {
            readers_count_lock->unlock();
            break;
        } else {
            readers_count_lock->unlock();
            usleep(500);
        }
    }
}

void MemoryManager::unlockWrite() {
    can_read_lock->lock();
    uint8_t* can_read_p = (uint8_t*) can_read->begin();
    *can_read_p = 1;
    can_read_lock->unlock();
}

const char* MemoryManager::getData(HashType hashType) {
    if (mem_ptrs.count(hashType) && mem_ptrs[hashType]) {
        throw SharedMemoryException("Try to get unreleased pointer.");
    }
    
    const Poco::File* data_fd = mem_fds[hashType];
    Poco::SharedMemory* data = new Poco::SharedMemory(*data_fd, Poco::SharedMemory::AM_READ);
    mem_ptrs[hashType] = data;
    return data->begin();
}

void MemoryManager::releaseData(HashType hashType) {
    if (!mem_ptrs.count(hashType) || !mem_ptrs[hashType]) {
        throw SharedMemoryException("Pointer is released yet.");
    }
    delete mem_ptrs[hashType];
    mem_ptrs.erase(hashType);
}

uint64_t MemoryManager::getDataSize(HashType hashType) {
    const Poco::File* data_fd = mem_fds[hashType];
    
    return data_fd->getSize();
}

void MemoryManager::setData(HashType hashType, const char* data, size_t size) {
    Poco::File* data_fd = mem_fds[hashType];
    
    data_fd->setSize(size);
    
    if (data) {
        Poco::SharedMemory mem_data(*data_fd, Poco::SharedMemory::AM_WRITE);
        memcpy(mem_data.begin(), data, size);
    }
}