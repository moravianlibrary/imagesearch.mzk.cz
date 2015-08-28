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
    session->setConnectionTimeout(30000);
    
    *session << "CREATE TABLE IF NOT EXISTS records ("
                "id TEXT PRIMARY KEY,"
                "thumbnail TEXT,"
                "metadata TEXT,"
                "blockHash BLOB,"
                "dHash BLOB,"
                "gaussDHash BLOB,"
                "gauss2DHash BLOB,"
                "gaussBlockHash BLOB)", now;
}

RecordManager::~RecordManager() {
    delete session;
}

void RecordManager::putRecord(const Record& record) {
    const std::vector<uint64_t>& blockHash = record.getHash(BlockHash);
    const std::vector<uint64_t>& dHash = record.getHash(DHash);
    const std::vector<uint64_t>& gaussDHash = record.getHash(GaussDHash);
    const std::vector<uint64_t>& gauss2DHash = record.getHash(Gauss2DHash);
    const std::vector<uint64_t>& gaussBlockHash = record.getHash(GaussBlockHash);
    
    Poco::Data::BLOB blockHashBLOB((unsigned char*) &blockHash[0], blockHash.size() * 8);
    Poco::Data::BLOB dHashBLOB((unsigned char*) &dHash[0], dHash.size() * 8);
    Poco::Data::BLOB gaussDHashBLOB((unsigned char*) &gaussDHash[0], gaussDHash.size() * 8);
    Poco::Data::BLOB gauss2DHashBLOB((unsigned char*) &gauss2DHash[0], gauss2DHash.size() * 8);
    Poco::Data::BLOB gaussBlockHashBLOB((unsigned char*) &gaussBlockHash[0], gaussBlockHash.size() * 8);
    
    *session << "INSERT OR REPLACE INTO records"
                "(id, thumbnail, metadata, blockHash, dHash, gaussDHash, gauss2DHash, gaussBlockHash)"
                "VALUES"
                "(?, ?, ?, ?, ?, ?, ?, ?)",
                useRef(record.getId()),
                useRef(record.getThumbnail()),
                useRef(record.getMetadata()),
                use(blockHashBLOB),
                use(dHashBLOB),
                use(gaussDHashBLOB),
                use(gauss2DHashBLOB),
                use(gaussBlockHashBLOB),
                now;
}

void RecordManager::deleteRecord(const Record& record) {
    *session << "DELETE FROM records WHERE id = ?", useRef(record.getId()), now;
}

Record RecordManager::getRecord(const std::string& id) {
    string thumbnail;
    string metadata;
    Poco::Data::BLOB blockHash;
    Poco::Data::BLOB dHash;
    Poco::Data::BLOB gaussDHash;
    Poco::Data::BLOB gauss2DHash;
    Poco::Data::BLOB gaussBlockHash;
    
    *session << "SELECT thumbnail, metadata, blockHash, dHash, gaussDHash, gauss2DHash, gaussBlockHash "
                "FROM records "
                "WHERE id = ?",
                into(thumbnail),
                into(metadata),
                into(blockHash),
                into(dHash),
                into(gaussDHash),
                into(gauss2DHash),
                into(gaussBlockHash),
                useRef(id),
                now;
    
    Record record;
    record.setId(id);
    record.setThumbnail(thumbnail);
    record.setMetadata(metadata);
    record.getHashes()[BlockHash] = convertTo64Vector(blockHash.rawContent(), blockHash.size());
    record.getHashes()[DHash] = convertTo64Vector(dHash.rawContent(), dHash.size());
    record.getHashes()[GaussDHash] = convertTo64Vector(gaussDHash.rawContent(), gaussDHash.size());
    record.getHashes()[Gauss2DHash] = convertTo64Vector(gauss2DHash.rawContent(), gauss2DHash.size());
    record.getHashes()[GaussBlockHash] = convertTo64Vector(gaussBlockHash.rawContent(), gaussBlockHash.size());
    
    return record;
}

std::vector<Record> RecordManager::getAllRecords() {
    Poco::Data::Statement select(*session);
    select << "SELECT id, thumbnail, metadata, blockHash, dHash, gaussDHash, gauss2DHash, gaussBlockHash FROM records", now;
    
    Poco::Data::RecordSet rs(select);
    
    vector<Record> result;
    for (Poco::Data::RecordSet::Iterator it = rs.begin(); it != rs.end(); it++) {
        Record record;
        Poco::Data::BLOB blockHash;
        Poco::Data::BLOB dHash;
        Poco::Data::BLOB gaussDHash;
        Poco::Data::BLOB gauss2DHash;
        Poco::Data::BLOB gaussBlockHash;
        
        record.setId(it->get(0).extract<string>());
        record.setThumbnail(it->get(1).extract<string>());
        record.setMetadata(it->get(2).extract<string>());
        
        blockHash = it->get(3).extract<Poco::Data::BLOB>();
        dHash = it->get(4).extract<Poco::Data::BLOB>();
        gaussDHash = it->get(5).extract<Poco::Data::BLOB>();
        gauss2DHash = it->get(6).extract<Poco::Data::BLOB>();
        gaussBlockHash = it->get(7).extract<Poco::Data::BLOB>();
        
        record.getHashes()[BlockHash] = convertTo64Vector(blockHash.rawContent(), blockHash.size());
        record.getHashes()[DHash] = convertTo64Vector(dHash.rawContent(), dHash.size());
        record.getHashes()[GaussDHash] = convertTo64Vector(gaussDHash.rawContent(), gaussDHash.size());
        record.getHashes()[Gauss2DHash] = convertTo64Vector(gauss2DHash.rawContent(), gauss2DHash.size());
        record.getHashes()[GaussBlockHash] = convertTo64Vector(gaussBlockHash.rawContent(), gaussBlockHash.size());
        
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