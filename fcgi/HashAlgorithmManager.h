/* 
 * File:   HashAlgorithmManager.h
 * Author: dudae
 *
 * Created on Utorok, 2015, júna 30, 17:16
 */

#ifndef HASHALGORITHMMANAGER_H
#define	HASHALGORITHMMANAGER_H

#include <vector>
#include <stdint.h>

#include "Record.h"
#include "HashAlgorithm.h"

class HashAlgorithmManager {
public:
    static HashAlgorithmManager& getInstance();
    
    HashAlgorithmManager();
    virtual ~HashAlgorithmManager();
    
    void attachHashes(Record& record, const std::vector<uint8_t>* image);
    void attachIdenticalHashes(Record& record, const std::vector<uint8_t>* image);
    void attachSimilarHashes(Record& record, const std::vector<uint8_t>* image);
    
    std::vector<HashType> getIdenticalHashes();
    std::vector<HashType> getSimilarHashes();
    std::vector<HashType> getHashes();
    HashAlgorithm& getHashAlgorithm(HashType);
    bool isIdenticalHash(HashType);
    bool isSimilarHash(HashType);
private:
    std::vector<HashAlgorithm*> identicalHashes;
    std::vector<HashAlgorithm*> similarHashes;
};

#endif	/* HASHALGORITHMMANAGER_H */

