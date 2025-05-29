#!/bin/bash

echo ""
echo "Applying migration VatRegisteredInEu"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /vatRegisteredInEu                        controllers.VatRegisteredInEuController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /vatRegisteredInEu                        controllers.VatRegisteredInEuController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeVatRegisteredInEu                  controllers.VatRegisteredInEuController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeVatRegisteredInEu                  controllers.VatRegisteredInEuController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "vatRegisteredInEu.title = vatRegisteredInEu" >> ../conf/messages.en
echo "vatRegisteredInEu.heading = vatRegisteredInEu" >> ../conf/messages.en
echo "vatRegisteredInEu.checkYourAnswersLabel = vatRegisteredInEu" >> ../conf/messages.en
echo "vatRegisteredInEu.error.required = Select yes if vatRegisteredInEu" >> ../conf/messages.en
echo "vatRegisteredInEu.change.hidden = VatRegisteredInEu" >> ../conf/messages.en

echo "Migration VatRegisteredInEu completed"
