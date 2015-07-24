/* 
 * File:   RecordManager.cpp
 * Author: dudae
 * 
 * Created on Pondelok, 2015, júna 29, 12:38
 */

#include "RecordManager.h"

#include <Poco/Data/SQLite/Connector.h>
#include <Poco/Data/RecordSet.h>
#include <fstream>

using Poco::Data::Keywords::now;
using Poco::Data::Keywords::use;
using Poco::Data::Keywords::useRef;
using Poco::Data::Keywords::into;

using namespace std;

RecordManager& RecordManager::getInstance() {
    static RecordManager instance;
    return instance;
}

RecordManager::RecordManager() {
    Poco::Data::SQLite::Connector::registerConnector();
    session = new Poco::Data::Session("SQLite", getenv("IMAGESEARCH_DB"));
    
    *session << "CREATE TABLE IF NOT EXISTS records ("
                "id TEXT PRIMARY KEY,"
                "title TEXT,"
                "numGcps INTEGER,"
                "rmsError REAL,"
                "blockHash BLOB,"
                "dHash BLOB,"
                "cannyDHash BLOB)", now;
}

RecordManager::~RecordManager() {
    delete session;
}

void RecordManager::putRecord(const Record& record) {
    const std::vector<uint64_t>& blockHash = record.getHash(BlockHash);
    const std::vector<uint64_t>& dHash = record.getHash(DHash);
    const std::vector<uint64_t>& cannyDHash = record.getHash(CannyDHash);
    
    Poco::Data::BLOB blockHashBLOB((unsigned char*) &blockHash[0], blockHash.size() * 8);
    Poco::Data::BLOB dHashBLOB((unsigned char*) &dHash[0], dHash.size() * 8);
    Poco::Data::BLOB cannyDHashBLOB((unsigned char*) &cannyDHash[0], cannyDHash.size() * 8);
    
    *session << "INSERT OR REPLACE INTO records"
                "(id, title, numGcps, rmsError, blockHash, dHash, cannyDHash)"
                "VALUES"
                "(?, ?, ?, ?, ?, ?, ?)",
                useRef(record.getId()),
                useRef(record.getTitle()),
                useRef(record.getNumGcps()),
                useRef(record.getRmsError()),
                use(blockHashBLOB),
                use(dHashBLOB),
                use(cannyDHashBLOB),
                now;
}

void RecordManager::deleteRecord(const Record& record) {
    *session << "DELETE FROM records WHERE id = ?", useRef(record.getId()), now;
}

Record RecordManager::getRecord(const std::string& id) {
    string title;
    size_t numGcps;
    double rmsError;
    Poco::Data::BLOB blockHash;
    Poco::Data::BLOB dHash;
    Poco::Data::BLOB cannyDHash;
    
    *session << "SELECT title, numGcps, rmsError, blockHash, dHash, cannyDHash "
                "FROM records "
                "WHERE id = ?",
                into(title),
                into(numGcps),
                into(rmsError),
                into(blockHash),
                into(dHash),
                into(cannyDHash),
                useRef(id),
                now;
    
    Record record;
    record.setId(id);
    record.setTitle(title);
    record.setNumGcps(numGcps);
    record.setRmsError(rmsError);
    record.getHashes()[BlockHash] = convertTo64Vector(blockHash.rawContent(), blockHash.size());
    record.getHashes()[DHash] = convertTo64Vector(dHash.rawContent(), dHash.size());
    record.getHashes()[CannyDHash] = convertTo64Vector(cannyDHash.rawContent(), cannyDHash.size());
    
    return record;
}

std::vector<Record> RecordManager::getAllRecords() {
    Poco::Data::Statement select(*session);
    select << "SELECT id, title, numGcps, rmsError, blockHash, dHash, cannyDHash FROM records", now;
    
    Poco::Data::RecordSet rs(select);
    
    vector<Record> result;
    for (Poco::Data::RecordSet::Iterator it = rs.begin(); it != rs.end(); it++) {
        Record record;
        Poco::Data::BLOB blockHash;
        Poco::Data::BLOB dHash;
        Poco::Data::BLOB cannyDHash;
        
        record.setId(it->get(0).extract<string>());
        record.setTitle(it->get(1).extract<string>());
        record.setNumGcps(it->get(2).extract<int>());
        record.setRmsError(it->get(3).extract<double>());
        
        blockHash = it->get(4).extract<Poco::Data::BLOB>();
        dHash = it->get(5).extract<Poco::Data::BLOB>();
        cannyDHash = it->get(6).extract<Poco::Data::BLOB>();
        
        record.getHashes()[BlockHash] = convertTo64Vector(blockHash.rawContent(), blockHash.size());
        record.getHashes()[DHash] = convertTo64Vector(dHash.rawContent(), dHash.size());
        record.getHashes()[CannyDHash] = convertTo64Vector(cannyDHash.rawContent(), cannyDHash.size());
        
        result.push_back(record);
    }
    return result;
}

std::vector<uint64_t> RecordManager::convertTo64Vector(const unsigned char* data, size_t size) {
    uint64_t* pointer = (uint64_t*) data;
    size_t length = size / 8;
    
    vector<uint64_t> result;
    for (size_t i = 0; i < length; i++) {
        result.push_back(*pointer);
        pointer++;
    }
    return result;
}