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
    
    const std::string& getTitle() const { return title; }
    void setTitle(const std::string& title) { this->title = title; }
    
    const size_t& getNumGcps() const { return numGcps; }
    void setNumGcps(const size_t numGcps) { this->numGcps = numGcps; }
    
    const double& getRmsError() const { return rmsError; }
    void setRmsError(const double rmsError) { this->rmsError = rmsError; }
    
    Poco::JSON::Object toJSON();
    
private:
    std::string id;
    std::map<HashType, std::vector<uint64_t> > hashes;
    std::string title;
    size_t numGcps;
    double rmsError;
};

std::ostream& operator<<(std::ostream &out, const Record& record);

#endif	/* RECORD_H */

