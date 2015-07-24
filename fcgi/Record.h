/* 
 * File:   Record.h
 * Author: dudae
 *
 * Created on Nedeľa, 2015, júna 28, 17:10
 */

#ifndef RECORD_H
#define	RECORD_H

#include <string>
#include <map>
#include <vector>
#include <ostream>
#include <stdint.h>

#include <Poco/JSON/Object.h>

#include "HashType.h"

class Record {
public:
    static const int MAX_ID_LEN = 60;
    
    Record();
    Record(const Record& orig);
    virtual ~Record();
    
    const std::string& getId() const { return id; }
    void setId(const std::string& id) { this->id = id; }
    
    std::map<HashType, std::vector<uint64_t> >& getHashes() { return hashes; }
    const std::vector<uint64_t>& getHash(HashType type) const { return hashes.at(type); }
    void setHashes(const std::map<HashType, std::vector<uint64_t> >& hashes) { this->hashes = hashes; }
    
    const std::string& getThumbnail() const { return thumbnail; }
    void setThumbnail(const std::string& thumbnail) { this->thumbnail = thumbnail; }
    
    const std::string& getMetadata() const { return metadata; }
    void setMetadata(const std::string& metadata) { this->metadata = metadata; }
    
    Poco::JSON::Object toJSON();
    
private:
    std::string id;
    std::map<HashType, std::vector<uint64_t> > hashes;
    std::string thumbnail;
    std::string metadata;
};

#endif	/* RECORD_H */

