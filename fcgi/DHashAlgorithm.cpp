/* 
 * File:   DHashAlgorithm.cpp
 * Author: dudae
 * 
 * Created on Pondelok, 2015, júna 29, 16:13
 */

#include "DHashAlgorithm.h"

#include <dhash.h>
#include <sstream>

#include "DHashException.h"

using namespace std;

DHashAlgorithm::~DHashAlgorithm() {
    
}

std::vector<uint64_t> DHashAlgorithm::compute(const vector<uint8_t>* data) {
    dhash_err* err = dhash_new_err();
    uint64_t hash = dhash_compute_blob(&((*data)[0]), data->size(), err);
    
    if (err->err_type != OK) {
        ostringstream os;
        os << "Error occured during compute dhash: " << err->description;
        dhash_free_err(err);
        throw DHashException(os.str());
    }
    dhash_free_err(err);
    
    vector<uint64_t> result;
    result.push_back(hash);
    return result;
}

HashType DHashAlgorithm::getType() {
    return DHash;
}

size_t DHashAlgorithm::getHashSize() {
    return 8;
}
