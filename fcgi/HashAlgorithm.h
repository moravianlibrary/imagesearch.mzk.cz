/* 
 * File:   HashAlgorithm.h
 * Author: dudae
 *
 * Created on Pondelok, 2015, júna 29, 15:54
 */

#ifndef HASHALGORITHM_H
#define	HASHALGORITHM_H

#include <vector>
#include <stdint.h>
#include <stddef.h>

#include "HashType.h"

class HashAlgorithm {
public:
    virtual ~HashAlgorithm() {}
    
    virtual std::vector<uint64_t> compute(const std::vector<uint8_t>* data) = 0;
    virtual HashType getType() = 0;
    virtual size_t getHashSize() = 0;
private:

};

#endif	/* HASHALGORITHM_H */

