/* 
 * File:   Record.cpp
 * Author: dudae
 * 
 * Created on Nedeľa, 2015, júna 28, 17:10
 */

#include "Record.h"

#include <iomanip>
#include <Poco/JSON/Parser.h>

using namespace std;

Record::Record() {
}

Record::Record(const Record& orig) {
    this->id = orig.id;
    this->hashes = orig.hashes;
    this->thumbnail = orig.thumbnail;
    this->metadata = orig.metadata;
}

Record::~Record() {
}

Poco::JSON::Object Record::toJSON() {
    Poco::JSON::Object result;
    result.set("id", id);
    result.set("thumbnail", thumbnail);
    if (!metadata.empty()) {
        Poco::JSON::Parser parser;
        result.set("metadata", parser.parse(metadata));
    }
    return result;
}
