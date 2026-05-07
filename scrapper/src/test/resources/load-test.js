import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
    stages: [
        {duration: '1m', target: 28},
        {duration: '5m', target: 28},
        {duration: '30s', target: 0}
    ]
};

const BASE_URL = "http://host.docker.internal:8081"

export default function (){
    const chatId = randomIntBetween(1, 1000);

    const isWrite = Math.random() < 0.01;
    const params = {
        headers: {'Content-Type': 'application/json', "Tg-Chat-Id": chatId}
    }
    if (isWrite){
        const randomLink = `https://github.com/VladLipaev/repo${randomIntBetween(1, 100000)}`;
        const payload = JSON.stringify({link: randomLink, tags: []});
        let res = http.post(`${BASE_URL}/links`, payload, params);
        check(res, {
            'POST status is 200 or 201': (r) => r.status === 200 || r.status === 201
        });
    }
    else{
        let res = http.get(`${BASE_URL}/links`, params);

        check(res, {
            'GET status is 200': (r) => r.status === 200
        });
    }

    sleep(0.1);
}
