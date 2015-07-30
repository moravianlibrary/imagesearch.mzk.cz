/* 
 * File:   Controller.cpp
 * Author: dudae
 * 
 * Created on Nedeľa, 2015, júna 28, 14:30
 */

#include "Controller.h"

#include <string>

#include <Poco/Dynamic/Var.h>
#include <Poco/JSON/Object.h>
#include <Poco/JSON/JSONException.h>

#include "appio.h"
#include "RequestException.h"
#include "RecordManager.h"
#include "HashAlgorithmManager.h"
#include "Image.h"
#include "HashManager.h"
#include "LogicalException.h"

using namespace std;

Controller::Controller() {
}

Controller::~Controller() {
}

void Controller::ingestRequest(const Poco::Dynamic::Var& request) {
    if (request.type() != typeid(Poco::JSON::Array::Ptr)) {
        throw RequestException("Invalid JSON format, root must be array.");
    }
    
    try {
        Poco::JSON::Array::Ptr jsonArray = request.extract<Poco::JSON::Array::Ptr>();
        
        for (Poco::JSON::Array::ConstIterator it = jsonArray->begin(); it != jsonArray->end(); it++) {
            Poco::JSON::Object::Ptr json = it->extract<Poco::JSON::Object::Ptr>();
            
            Record record;
            vector<uint8_t>* image_data = NULL;
            
            if (json->has("id")) {
                record.setId(json->getValue<string>("id"));
            }
            if (json->has("thumbnail")) {
                record.setThumbnail(json->getValue<string>("thumbnail"));
            }
            if (json->has("metadata")) {
                record.setMetadata(json->getValue<string>("metadata"));
            }
            if (json->has("blockHash")) {
                record.getHashes()[BlockHash] = str2bin(json->getValue<string>("blockHash"));
            }
            if (json->has("dHash")) {
                record.getHashes()[DHash] = str2bin(json->getValue<string>("dHash"));
            }
            if (json->has("cannyDHash")) {
                record.getHashes()[CannyDHash] = str2bin(json->getValue<string>("cannyDHash"));
            }
            if (json->has("image_base64")) {
                image_data = Image::fromBase64(json->getValue<string>("image_base64"));
            }
            if (!image_data && json->has("image_url")) {
                image_data = Image::fromUrl(json->getValue<string>("image_url"));
            }
            
            if (json->has("status")) {
                string status = json->getValue<string>("status");
                if (status == "deleted") {
                    deleteRequest(record);
                    continue;
                } else {
                    throw RequestException("Invalid status value.");
                }
            }

            if (image_data) {
                HashAlgorithmManager::getInstance().attachHashes(record, image_data);
                delete image_data;
            }
            putRequest(record);
        }
    } catch (Poco::JSON::JSONException& e) {
        throw RequestException("Invalid JSON format.");
    }
    appio::print_ok("Data was successfully changed.");
}
    
void Controller::commitRequest() {
    std::vector<Record> records = RecordManager::getInstance().getAllRecords();
    HashManager::getInstance().update(records);
    appio::print_ok("Successfully commited.");
}

void Controller::searchIdenticalRequest(std::map<std::string, std::string>& params, const Poco::Dynamic::Var& request) {
    Record record;
    vector<uint8_t>* image_data = NULL;
    
    if (params.count("blockHash")) {
        record.getHashes()[BlockHash] = str2bin(params["blockHash"]);
    }
    if (params.count("dHash")) {
        record.getHashes()[DHash] = str2bin(params["dHash"]);
    }
    if (params.count("cannyDHash")) {
        record.getHashes()[CannyDHash] = str2bin(params["cannyDHash"]);
    }
    if (params.count("url")) {
        image_data = Image::fromUrl(params["url"]);
    }
    
    if (!request.isEmpty()) {
        Poco::JSON::Object::Ptr json = request.extract<Poco::JSON::Object::Ptr>();
        if (!image_data && json->has("image_base64")) {
            image_data = Image::fromBase64(json->getValue<string>("image_base64"));
        }
    }
    
    if (image_data) {
        HashAlgorithmManager::getInstance().attachIdenticalHashes(record, image_data);
        delete image_data;
    }
    
    appio::print_ok(HashManager::getInstance().searchIdentical(record));
}

void Controller::searchSimilarRequest(std::map<std::string, std::string>& params, const Poco::Dynamic::Var& request) {
    Record record;
    vector<uint8_t>* image_data = NULL;
    int count = 10;
    
    if (params.count("blockHash")) {
        record.getHashes()[BlockHash] = str2bin(params["blockHash"]);
    }
    if (params.count("dHash")) {
        record.getHashes()[DHash] = str2bin(params["dHash"]);
    }
    if (params.count("cannyDHash")) {
        record.getHashes()[CannyDHash] = str2bin(params["cannyDHash"]);
    }
    if (params.count("url")) {
        image_data = Image::fromUrl(params["url"]);
    }
    if (params.count("count")) {
        count = atoi(params["count"].c_str());
    }
    
    if (!request.isEmpty()) {
        Poco::JSON::Object::Ptr json = request.extract<Poco::JSON::Object::Ptr>();
        if (!image_data && json->has("image_base64")) {
            image_data = Image::fromBase64(json->getValue<string>("image_base64"));
        }
    }
    
    if (image_data) {
        HashAlgorithmManager::getInstance().attachSimilarHashes(record, image_data);
        delete image_data;
    }
    
    appio::print_ok(HashManager::getInstance().searchSimilar(record, count));
}

void Controller::putRequest(const Record& record) {
    RecordManager::getInstance().putRecord(record);
}

void Controller::deleteRequest(const Record& record) {
    RecordManager::getInstance().deleteRecord(record);
}

inline int Controller::char2bin(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    
    ostringstream os;
    os << "Controller::char2bin: illegal char " << c;
    throw LogicalException(os.str());
}

std::vector<uint64_t> Controller::str2bin(const string& str) {
    int len = str.length()/2;
    uint8_t* result = new uint8_t[len];
    for (int i = 0; i < len; i++) {
        uint8_t b1 = str[i*2];
        uint8_t b2 = str[i*2+1];
        b1 = char2bin(b1);
        b2 = char2bin(b2);
        b1 = b1 << 4;
        result[len - i - 1] = b1 | b2;
    }
    int vlen = len / 8;
    vector<uint64_t> vresult;
    uint64_t* p = (uint64_t*) result;
    p += vlen - 1;
    for (int i = 0; i < vlen; i++) {
        vresult.push_back(*p);
        p--;
    }
    delete [] result;
    return vresult;
}