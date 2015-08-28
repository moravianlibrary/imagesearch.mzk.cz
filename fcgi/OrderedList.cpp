/* 
 * File:   OrderedList.cpp
 * Author: dudae
 * 
 * Created on Utorok, 2015, júla 21, 15:04
 */

#include "OrderedList.h"

using namespace std;

OrderedList::OrderedList(int size):size(size) {
}

OrderedList::OrderedList(const OrderedList& orig) {
}

OrderedList::~OrderedList() {
}

void OrderedList::push(std::pair<std::string, double> item) {
    if (exists.count(item.first)) {
        remove(item.first);
        item.second *= exists[item.first];
    }
    if (!data.empty()) {
        pair<string, double> last = data.back();
        if (last.second < item.second) {
            if (data.size() < size) {
                data.push_back(item);
                exists[item.first] = item.second;
            } else {
                return;
            }
        }
    } else {
        data.push_back(item);
        exists[item.first] = item.second;
        return;
    }
    for (list<pair<string, double> >::iterator it = data.begin(); it != data.end(); it++) {
        if (item.second < it->second) {
            data.insert(it, item);
            exists[item.first] = item.second;
            break;
        }
    }
    if (data.size() > size) {
        exists.erase(data.back().first);
        data.remove(data.back());
    }
}

const std::list<std::pair<std::string, double> >& OrderedList::getList() const {
    return data;
}

void OrderedList::remove(std::string key) {
    for (list<pair<string, double> >::iterator it = data.begin(); it != data.end(); it++) {
        if (it->first == key) {
            data.erase(it);
            break;
        }
    }
}