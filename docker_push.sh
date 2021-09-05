if [ "$TRAVIS_EVENT_TYPE" != "cron" ]
then
echo "$ARTIFACT_REGISTRY_KEY" | docker login -u _json_key_base64 --password-stdin https://us-west1-docker.pkg.dev
export SHRINE_REPO=us-west1-docker.pkg.dev/som-rit-infrastructure-prod/i2b2/shrine
docker build -f Dockerfile -t $SHRINE_REPO:build-$TRAVIS_BUILD_NUMBER .
echo $SHRINE_REPO:build-$TRAVIS_BUILD_NUMBER
docker push $SHRINE_REPO
fi
