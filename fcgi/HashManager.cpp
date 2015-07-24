/* 
 * File:   HashManager.cpp
 * Author: dudae
 * 
 * Created on Štvrtok, 2015, júla 9, 13:42
 */

#include <string.h>
#include <iostream>
#include <limits>

#include "HashManager.h"
#include "MemoryManager.h"
#include "HashAlgorithmManager.h"
#include "SharedMemoryException.h"
#include "OrderedList.h"

using namespace std;

HashManager& HashManager::getInstance() {
    static HashManager instance;
    return instance;
}

HashManager::HashManager() {
    
}

HashManager::~HashManager() {
    
}
    
SearchResult HashManager::searchIdentical(Record& record) const {
    MemoryManager::getInstance().lockRead();
    
    vector<pair<string, double> > results;
    map<HashType, vector<uint64_t> > hashes = record.getHashes();
    for (map<HashType, vector<uint64_t> >::const_iterator it = hashes.begin(); it != hashes.end(); it++) {
        if (!HashAlgorithmManager::getInstance().isIdenticalHash(it->first)) {
            continue;
        }
        size_t id_size = Record::MAX_ID_LEN;
        size_t hash_size = HashAlgorithmManager::getInstance().getHashAlgorithm(it->first).getHashSize();
        int data_size = MemoryManager::getInstance().getDataSize(it->first);
        int record_size = id_size + hash_size;
        
        if (data_size % record_size != 0) {
            MemoryManager::getInstance().releaseData(it->first);
            MemoryManager::getInstance().unlockRead();
            throw SharedMemoryException("Invalid shared data.");
        }
        
        int records_count = data_size / record_size;
        const char* data_p = MemoryManager::getInstance().getData(it->first);
        
        double min_distance = numeric_limits<double>::max();
        string min_id;
        
        for (int i = 0; i < records_count; i++) {
            const char* id = data_p; data_p += id_size;
            const uint8_t* hash = (const uint8_t*) data_p; data_p += hash_size;
            
            double distance = hm_distance(hash, (uint8_t*) &it->second[0], hash_size);
            if (distance < min_distance) {
                min_distance = distance;
                min_id = id;
            }
        }
        results.push_back(pair<string, double>(min_id, min_distance));
        MemoryManager::getInstance().releaseData(it->first);
    }
    MemoryManager::getInstance().unlockRead();
    
    SearchResult result;
    result.setFound(true);
    result.setDistance(1);
    for (vector<pair<string, double> >::const_iterator it = results.begin(); it != results.end(); it++) {
        if (result.getId().empty()) {
            result.setId(it->first);
        } else {
            if (result.getId() != it->first) {
                result.setFound(false);
                break;
            }
        }
        result.setDistance(result.getDistance() * it->second);
    }
    
    
    return result;
}

std::vector<SearchResult> HashManager::searchSimilar(Record& record, int count) const {
    MemoryManager::getInstance().lockRead();
    
    OrderedList similar_records(count);
    map<HashType, vector<uint64_t> > hashes = record.getHashes();
    for (map<HashType, vector<uint64_t> >::const_iterator it = hashes.begin(); it != hashes.end(); it++) {
        if (!HashAlgorithmManager::getInstance().isSimilarHash(it->first)) {
            continue;
        }
        size_t id_size = Record::MAX_ID_LEN;
        size_t hash_size = HashAlgorithmManager::getInstance().getHashAlgorithm(it->first).getHashSize();
        int data_size = MemoryManager::getInstance().getDataSize(it->first);
        int record_size = id_size + hash_size;
        
        if (data_size % record_size != 0) {
            MemoryManager::getInstance().releaseData(it->first);
            MemoryManager::getInstance().unlockRead();
            throw SharedMemoryException("Invalid shared data.");
        }
        
        int records_count = data_size / record_size;
        const char* data_p = MemoryManager::getInstance().getData(it->first);
        
        for (int i = 0; i < records_count; i++) {
            const char* id = data_p; data_p += id_size;
            const uint8_t* hash = (const uint8_t*) data_p; data_p += hash_size;
            
            double distance = hm_distance(hash, (uint8_t*) &it->second[0], hash_size);
            
            similar_records.push(pair<string, double>(string(id), distance));
        }
        MemoryManager::getInstance().releaseData(it->first);
    }
    MemoryManager::getInstance().unlockRead();
    
    vector<SearchResult> result;

    for (list<pair<string, double> >::const_iterator it = similar_records.getList().begin(); it != similar_records.getList().end(); it++) {
        SearchResult searchResult;
        searchResult.setId(it->first);
        searchResult.setDistance(it->second);
        result.push_back(searchResult);
    }
    
    return result;
}

void HashManager::update(const std::vector<Record>& records) {
    MemoryManager::getInstance().lockWrite();
    vector<HashType> hashes = HashAlgorithmManager::getInstance().getHashes();
    
    for (vector<HashType>::const_iterator it = hashes.begin(); it != hashes.end(); it++) {
        char* data = NULL;
        uint64_t size;
        if (records.size()) {
            prepare_data(records, *it, &data, size);
        } else {
            prepare_empty_data(&data, size);
        }
        MemoryManager::getInstance().setData(*it, data, size);
        delete[] data;
    }
    MemoryManager::getInstance().unlockWrite();
}

void HashManager::prepare_data(const std::vector<Record>& records, HashType hashType, char** data, uint64_t& size) {
    size_t hash_size = HashAlgorithmManager::getInstance().getHashAlgorithm(hashType).getHashSize();
    size_t id_size = Record::MAX_ID_LEN;
    size_t record_size = id_size + hash_size;
    size_t record_counts = records.size();
    
    size = record_size * record_counts;
    *data = new char[size];
    
    char* data_p = *data;
    
    for (vector<Record>::const_iterator it = records.begin(); it != records.end(); it++) {
        const Record& record = *it;
        // Copy id
        memcpy(data_p, record.getId().c_str(), id_size);
        data_p[id_size - 1] = '\0';
        data_p += id_size;
        // Copy hash
        memcpy(data_p, &record.getHash(hashType)[0], hash_size);
        data_p += hash_size;
    }
}

void HashManager::prepare_empty_data(char** data, uint64_t& size) {
    size = 0;
    *data = NULL;
}

double HashManager::hm_distance(const uint8_t *hashA, const uint8_t *hashB, int len) {
    if ((hashA == NULL) || (hashB == NULL) || (len <= 0)){
	return -1.0;
    }
    double dist = 0;
    uint8_t D = 0;
    for (int i = 0; i < len; i++){
	D = hashA[i]^hashB[i];
	dist = dist + (double) bitcount8(D);
    }
    double bits = (double) len * 8;
    return dist / bits;
}

int HashManager::bitcount8(uint8_t val) {
    int num = 0;
    while (val){
	++num;
	val &= val - 1;
    }
    return num;
}