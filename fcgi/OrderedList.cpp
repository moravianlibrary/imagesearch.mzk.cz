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

void OrderedList::push(const std::pair<std::string, double>& item) {
    if (!data.empty()) {
        pair<string, double> last = data.back();
        if (last.second < item.second) {
            if (data.size() < size) {
                data.push_back(item);
            } else {
                return;
            }
        }
    } else {
        data.push_back(item);
        return;
    }
    for (list<pair<string, double> >::iterator it = data.begin(); it != data.end(); it++) {
        if (item.second < it->second) {
            data.insert(it, item);
            break;
        }
    }
    if (data.size() > size) {
        data.remove(data.back());
    }
}

const std::list<std::pair<std::string, double> >& OrderedList::getList() const {
    return data;
}