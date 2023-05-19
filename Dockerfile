FROM openjdk:17

COPY ./out/production/bully-algorithm/* .

CMD ['java', 'BullyAlgorithm']