if [ "$TRAVIS_EVENT_TYPE" != "cron" ]
then
echo "$DOCKER_API_TOKEN" | docker login -u "$DOCKER_USERNAME" --password-stdin somrit/tide
export SHRINE_REPO=somrit/tide
docker build -f Dockerfile -t $SHRINE_REPO:build-$TRAVIS_BUILD_NUMBER .
echo $SHRINE_REPO:build-$TRAVIS_BUILD_NUMBER
docker push $SHRINE_REPO
fi
