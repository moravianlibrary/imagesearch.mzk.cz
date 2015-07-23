/* 
 * File:   BlockHashAlgorithm.h
 * Author: dudae
 *
 * Created on Utorok, 2015, júna 30, 16:45
 */

#ifndef BLOCKHASHALGORITHM_H
#define	BLOCKHASHALGORITHM_H

#include <stddef.h>
#include "HashAlgorithm.h"


class BlockHashAlgorithm : public HashAlgorithm {
public:
    virtual ~BlockHashAlgorithm();
    
    virtual std::vector<uint64_t> compute(const std::vector<uint8_t>* data);
    virtual HashType getType();
    virtual size_t getHashSize();
private:

};

#endif	/* BLOCKHASHALGORITHM_H */

