#!/bin/bash

echo ""
echo "Applying migration HasFixedEstablishment"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /hasFixedEstablishment                        controllers.HasFixedEstablishmentController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /hasFixedEstablishment                        controllers.HasFixedEstablishmentController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeHasFixedEstablishment                  controllers.HasFixedEstablishmentController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeHasFixedEstablishment                  controllers.HasFixedEstablishmentController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "hasFixedEstablishment.title = hasFixedEstablishment" >> ../conf/messages.en
echo "hasFixedEstablishment.heading = hasFixedEstablishment" >> ../conf/messages.en
echo "hasFixedEstablishment.checkYourAnswersLabel = hasFixedEstablishment" >> ../conf/messages.en
echo "hasFixedEstablishment.error.required = Select yes if hasFixedEstablishment" >> ../conf/messages.en
echo "hasFixedEstablishment.change.hidden = HasFixedEstablishment" >> ../conf/messages.en

echo "Migration HasFixedEstablishment completed"
