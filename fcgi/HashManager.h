/* 
 * File:   HashManager.h
 * Author: dudae
 *
 * Created on Štvrtok, 2015, júla 9, 13:42
 */

#ifndef HASHMANAGER_H
#define	HASHMANAGER_H

#include <vector>

#include "SearchResult.h"
#include "Record.h"


class HashManager {
public:
    static HashManager& getInstance(); 
    virtual ~HashManager();
    
    SearchResult searchIdentical(Record& record) const;
    std::vector<SearchResult> searchSimilar(Record& record) const;
    
    void update(const std::vector<Record>& records);
private:
    HashManager();
    
    static void prepare_data(const std::vector<Record>& records, HashType hashType, char** data, uint64_t& size);
    static void prepare_empty_data(char** data, uint64_t& size);
    static double hm_distance(const uint8_t *hashA, const uint8_t *hashB, int len);
    static int bitcount8(uint8_t val);
};

#endif	/* HASHMANAGER_H */

