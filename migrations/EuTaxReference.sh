#!/bin/bash

echo ""
echo "Applying migration EuTaxReference"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /euTaxReference                  controllers.EuTaxReferenceController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /euTaxReference                  controllers.EuTaxReferenceController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeEuTaxReference                        controllers.EuTaxReferenceController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeEuTaxReference                        controllers.EuTaxReferenceController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "euTaxReference.title = EuTaxReference" >> ../conf/messages.en
echo "euTaxReference.heading = EuTaxReference" >> ../conf/messages.en
echo "euTaxReference.checkYourAnswersLabel = EuTaxReference" >> ../conf/messages.en
echo "euTaxReference.error.nonNumeric = Enter your euTaxReference using numbers" >> ../conf/messages.en
echo "euTaxReference.error.required = Enter your euTaxReference" >> ../conf/messages.en
echo "euTaxReference.error.wholeNumber = Enter your euTaxReference using whole numbers" >> ../conf/messages.en
echo "euTaxReference.error.outOfRange = EuTaxReference must be between {0} and {1}" >> ../conf/messages.en
echo "euTaxReference.change.hidden = EuTaxReference" >> ../conf/messages.en

echo "Migration EuTaxReference completed"
