/* 
 * File:   SearchResult.h
 * Author: dudae
 *
 * Created on Štvrtok, 2015, júla 9, 14:30
 */

#ifndef SEARCHRESULT_H
#define	SEARCHRESULT_H

#include <string>

class SearchResult {
public:
    SearchResult():found(false),id(),distance(0) {}
    virtual ~SearchResult() {}
    
    bool isFound() const { return found; }
    void setFound(bool found) { this->found = found; }
    
    std::string getId() const { return id; }
    void setId(const std::string& id) { this->id = id; }
    
    double getDistance() const { return distance; }
    void setDistance(double distance) { this->distance = distance; }
private:
    bool found;
    std::string id;
    double distance;
};

#endif	/* SEARCHRESULT_H */

