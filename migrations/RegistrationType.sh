#!/bin/bash

echo ""
echo "Applying migration RegistrationType"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /registrationType                        controllers.RegistrationTypeController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /registrationType                        controllers.RegistrationTypeController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeRegistrationType                  controllers.RegistrationTypeController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeRegistrationType                  controllers.RegistrationTypeController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "registrationType.title = registrationType" >> ../conf/messages.en
echo "registrationType.heading = registrationType" >> ../conf/messages.en
echo "registrationType.option1 = Option 1" >> ../conf/messages.en
echo "registrationType.option2 = Option 2" >> ../conf/messages.en
echo "registrationType.checkYourAnswersLabel = registrationType" >> ../conf/messages.en
echo "registrationType.error.required = Select registrationType" >> ../conf/messages.en
echo "registrationType.change.hidden = RegistrationType" >> ../conf/messages.en

echo "Adding to ModelGenerators"
awk '/trait ModelGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryRegistrationType: Arbitrary[RegistrationType] =";\
    print "    Arbitrary {";\
    print "      Gen.oneOf(RegistrationType.values.toSeq)";\
    print "    }";\
    next }1' ../test-utils/generators/ModelGenerators.scala > tmp && mv tmp ../test-utils/generators/ModelGenerators.scala

echo "Migration RegistrationType completed"
