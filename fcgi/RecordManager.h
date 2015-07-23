/* 
 * File:   RecordManager.h
 * Author: dudae
 *
 * Created on Pondelok, 2015, júna 29, 12:38
 */

#ifndef RECORDMANAGER_H
#define	RECORDMANAGER_H

#include <string>
#include <vector>

#include <Poco/Data/Session.h>

#include "Record.h"

class RecordManager {
public:
    static RecordManager& getInstance();
    virtual ~RecordManager();
    
    void putRecord(const Record& record);
    void deleteRecord(const Record& record);
    Record getRecord(const std::string& id);
    std::vector<Record> getAllRecords();

private:
    RecordManager();
    RecordManager(const RecordManager& orig);
    std::vector<uint64_t> convertTo64Vector(const unsigned char* data, size_t size);
    
    Poco::Data::Session* session;
};

#endif	/* RECORDMANAGER_H */

