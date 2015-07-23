/* 
 * File:   DHashAlgorithm.h
 * Author: dudae
 *
 * Created on Pondelok, 2015, júna 29, 16:13
 */

#ifndef DHASHALGORITHM_H
#define	DHASHALGORITHM_H

#include <vector>

#include "HashAlgorithm.h"


class DHashAlgorithm : public HashAlgorithm {
public:
    virtual ~DHashAlgorithm();
    
    virtual std::vector<uint64_t> compute(const std::vector<uint8_t>* data);
    virtual HashType getType();
    virtual size_t getHashSize();

};

#endif	/* DHASHALGORITHM_H */

