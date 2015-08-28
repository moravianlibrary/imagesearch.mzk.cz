/* 
 * File:   OrderedList.h
 * Author: dudae
 *
 * Created on Utorok, 2015, júla 21, 15:04
 */

#ifndef ORDEREDLIST_H
#define	ORDEREDLIST_H

#include <list>
#include <map>
#include <string>

class OrderedList {
public:
    OrderedList(int size);
    OrderedList(const OrderedList& orig);
    virtual ~OrderedList();
    
    void push(std::pair<std::string, double> item);
    const std::list<std::pair<std::string, double> >& getList() const;
protected:
    void remove(std::string key);
private:
    size_t size;
    std::list<std::pair<std::string, double> > data;
    std::map<std::string, double> exists;
};

#endif	/* ORDEREDLIST_H */

