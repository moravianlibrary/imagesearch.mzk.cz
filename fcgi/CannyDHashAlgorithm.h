/* 
 * File:   CannyDHash.h
 * Author: dudae
 *
 * Created on Štvrtok, 2015, júla 9, 10:12
 */

#ifndef CANNYDHASH_H
#define	CANNYDHASH_H

#include "HashAlgorithm.h"
#include "HashType.h"


class CannyDHashAlgorithm : public HashAlgorithm {
public:
    virtual ~CannyDHashAlgorithm();
    
    virtual std::vector<uint64_t> compute(const std::vector<uint8_t>* data);
    virtual HashType getType();
    virtual size_t getHashSize();
private:

};

#endif	/* CANNYDHASH_H */

