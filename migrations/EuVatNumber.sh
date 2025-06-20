#!/bin/bash

echo ""
echo "Applying migration EuVatNumber"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /euVatNumber                  controllers.EuVatNumberController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /euVatNumber                  controllers.EuVatNumberController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeEuVatNumber                        controllers.EuVatNumberController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeEuVatNumber                        controllers.EuVatNumberController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "euVatNumber.title = EuVatNumber" >> ../conf/messages.en
echo "euVatNumber.heading = EuVatNumber" >> ../conf/messages.en
echo "euVatNumber.checkYourAnswersLabel = EuVatNumber" >> ../conf/messages.en
echo "euVatNumber.error.nonNumeric = Enter your euVatNumber using numbers" >> ../conf/messages.en
echo "euVatNumber.error.required = Enter your euVatNumber" >> ../conf/messages.en
echo "euVatNumber.error.wholeNumber = Enter your euVatNumber using whole numbers" >> ../conf/messages.en
echo "euVatNumber.error.outOfRange = EuVatNumber must be between {0} and {1}" >> ../conf/messages.en
echo "euVatNumber.change.hidden = EuVatNumber" >> ../conf/messages.en

echo "Migration EuVatNumber completed"
