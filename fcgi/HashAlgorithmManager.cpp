/* 
 * File:   HashAlgorithmManager.cpp
 * Author: dudae
 * 
 * Created on Utorok, 2015, júna 30, 17:16
 */

#include "HashAlgorithmManager.h"
#include "DHashAlgorithm.h"
#include "BlockHashAlgorithm.h"
#include "CannyDHashAlgorithm.h"
#include "LogicalException.h"

#include <iostream>
#include <iomanip>

using namespace std;

HashAlgorithmManager& HashAlgorithmManager::getInstance() {
    static HashAlgorithmManager instance;
    return instance;
}

HashAlgorithmManager::HashAlgorithmManager() {
    identicalHashes.push_back(new DHashAlgorithm());
    identicalHashes.push_back(new BlockHashAlgorithm());
    
    similarHashes.push_back(new CannyDHashAlgorithm());
}

HashAlgorithmManager::~HashAlgorithmManager() {
    for (vector<HashAlgorithm*>::const_iterator it = identicalHashes.begin(); it != identicalHashes.end(); it++) {
        delete *it;
    }
    for (vector<HashAlgorithm*>::const_iterator it = similarHashes.begin(); it != similarHashes.end(); it++) {
        delete *it;
    }
    identicalHashes.clear();
    similarHashes.clear();
}

void HashAlgorithmManager::attachHashes(Record& record, const std::vector<uint8_t>* image) {
    attachIdenticalHashes(record, image);
    attachSimilarHashes(record, image);
}

void HashAlgorithmManager::attachIdenticalHashes(Record& record, const std::vector<uint8_t>* image) {
    for (vector<HashAlgorithm*>::const_iterator it = identicalHashes.begin(); it != identicalHashes.end(); it++) {
        vector<uint64_t> data = (*it)->compute(image);
        record.getHashes()[(*it)->getType()] = (*it)->compute(image);
    }
}

void HashAlgorithmManager::attachSimilarHashes(Record& record, const std::vector<uint8_t>* image) {
    for (vector<HashAlgorithm*>::const_iterator it = similarHashes.begin(); it != similarHashes.end(); it++) {
        record.getHashes()[(*it)->getType()] = (*it)->compute(image);
    }
}

std::vector<HashType> HashAlgorithmManager::getIdenticalHashes() {
    vector<HashType> result;
    
    for (vector<HashAlgorithm*>::const_iterator it = identicalHashes.begin(); it != identicalHashes.end(); it++) {
        result.push_back((*it)->getType());
    }
    return result;
}

std::vector<HashType> HashAlgorithmManager::getSimilarHashes() {
    vector<HashType> result;
    
    for (vector<HashAlgorithm*>::const_iterator it = similarHashes.begin(); it != similarHashes.end(); it++) {
        result.push_back((*it)->getType());
    }
    return result;
}

std::vector<HashType> HashAlgorithmManager::getHashes() {
    vector<HashType> result;
    
    for (vector<HashAlgorithm*>::const_iterator it = identicalHashes.begin(); it != identicalHashes.end(); it++) {
        result.push_back((*it)->getType());
    }
    for (vector<HashAlgorithm*>::const_iterator it = similarHashes.begin(); it != similarHashes.end(); it++) {
        result.push_back((*it)->getType());
    }
    return result;
}

HashAlgorithm& HashAlgorithmManager::getHashAlgorithm(HashType hashType) {
    for (vector<HashAlgorithm*>::const_iterator it = identicalHashes.begin(); it != identicalHashes.end(); it++) {
        if ((*it)->getType() == hashType) {
            return **it;
        }
    }
    for (vector<HashAlgorithm*>::const_iterator it = similarHashes.begin(); it != similarHashes.end(); it++) {
        if ((*it)->getType() == hashType) {
            return **it;
        }
    }
    throw LogicalException("HashAlgorithmManager::getHashAlgorithm: hashType not found.");
}

bool HashAlgorithmManager::isIdenticalHash(HashType hashType) {
    for (vector<HashAlgorithm*>::const_iterator it = identicalHashes.begin(); it != identicalHashes.end(); it++) {
        if ((*it)->getType() == hashType) {
            return true;
        }
    }
    return false;
}

bool HashAlgorithmManager::isSimilarHash(HashType hashType) {
    for (vector<HashAlgorithm*>::const_iterator it = similarHashes.begin(); it != similarHashes.end(); it++) {
        if ((*it)->getType() == hashType) {
            return true;
        }
    }
    return false;
}