/* 
 * File:   Record.cpp
 * Author: dudae
 * 
 * Created on Nedeľa, 2015, júna 28, 17:10
 */

#include "Record.h"

#include <iomanip>

using namespace std;

Record::Record() {
    numGcps = 0;
    rmsError = 0;
}

std::ostream& operator<<(std::ostream &out, const Record& record) {
    out << "Record {"
        << "\tid: " << record.getId()
        << "\ttitle: " << record.getTitle()
        << "\tnumGcps: " << record.getNumGcps()
        << "\trmsError: " << record.getRmsError()
        << "\tblockHash:" << hex << record.getHash(BlockHash)[0]
        << std::endl;
    return out;
}

Record::Record(const Record& orig) {
    this->id = orig.id;
    this->hashes = orig.hashes;
    this->title = orig.title;
    this->numGcps = orig.numGcps;
    this->rmsError = orig.rmsError;
}

Record::~Record() {
}

Poco::JSON::Object Record::toJSON() {
    Poco::JSON::Object result;
    result.set("id", id);
    result.set("title", title);
    result.set("numGcps", numGcps);
    result.set("rmsError", rmsError);
    return result;
}
