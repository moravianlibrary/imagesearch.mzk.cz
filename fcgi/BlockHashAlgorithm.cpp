/* 
 * File:   BlockHashAlgorithm.cpp
 * Author: dudae
 * 
 * Created on Utorok, 2015, júna 30, 16:45
 */

#include "BlockHashAlgorithm.h"

#include <blockhash.h>

using namespace std;

BlockHashAlgorithm::~BlockHashAlgorithm() {
}

std::vector<uint64_t> BlockHashAlgorithm::compute(const vector<uint8_t>* data) {
    return blockhash::compute(*data, 256);
}

HashType BlockHashAlgorithm::getType() {
    return BlockHash;
}

size_t BlockHashAlgorithm::getHashSize() {
    return 32;
}

