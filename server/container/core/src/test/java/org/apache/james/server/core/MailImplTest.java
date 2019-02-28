/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.server.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.core.MaybeSender;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.mailet.ContractMailTest;
import org.apache.mailet.Mail;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.MailUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.fge.lambdas.Throwing;

public class MailImplTest extends ContractMailTest {

    @Override
    public MailImpl newMail() {
        return MailImpl.builder().name("mail-id").build();
    }

    private MimeMessage emptyMessage;

    @BeforeEach
    public void setup() throws MessagingException {
        emptyMessage = MimeMessageBuilder.mimeMessageBuilder()
            .setText("")
            .build();
    }

    @Test
    public void mailImplShouldHaveSensibleInitialValues() throws MessagingException {
        MailImpl mail = newMail();

        assertThat(mail.getName()).isEqualTo("mail-id");
        assertThat(mail.hasAttributes()).describedAs("no initial attributes").isFalse();
        assertThat(mail.getErrorMessage()).describedAs("no initial error").isNull();
        assertThat(mail.getLastUpdated()).isCloseTo(new Date(), TimeUnit.SECONDS.toMillis(1));
        assertThat(mail.getRecipients()).describedAs("no initial recipient").isNullOrEmpty();
        assertThat(mail.getRemoteAddr()).describedAs("initial remote address is localhost ip").isEqualTo("127.0.0.1");
        assertThat(mail.getRemoteHost()).describedAs("initial remote host is localhost").isEqualTo("localhost");
        assertThat(mail.getState()).describedAs("default initial state").isEqualTo(Mail.DEFAULT);
        assertThat(mail.getMessage()).isNull();
        assertThat(mail.getMaybeSender()).isEqualTo(MaybeSender.nullSender());
    }

    @Test
    public void mailImplShouldThrowWhenComputingSizeOnDefaultInstance() throws MessagingException {
        MailImpl mail = newMail();

        assertThatThrownBy(mail::getMessageSize).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void mailImplConstructionShouldSetDefaultValuesOnUnspecifiedFields() throws MessagingException {
        MailImpl mail = MailImpl.builder()
            .name(MailUtil.newId())
            .sender("sender@localhost")
            .build();

        MailImpl expected = newMail();
        assertThat(mail).isEqualToIgnoringGivenFields(expected, "sender", "name", "recipients", "lastUpdated");
        assertThat(mail.getLastUpdated()).isCloseTo(new Date(), TimeUnit.SECONDS.toMillis(1));
    }

    @Test
    public void mailImplConstructionShouldSetSpecifiedFields() throws MessagingException {
        String sender = "sender@localhost";
        String name = MailUtil.newId();
        MailImpl mail = MailImpl.builder()
            .name(name)
            .sender(sender)
            .build();

        assertThat(mail.getMaybeSender().get().asString()).isEqualTo(sender);
        assertThat(mail.getName()).isEqualTo(name);

     }

    @Test
    public void mailImplConstructionWithMimeMessageShouldSetSpecifiedFields() throws MessagingException {
        String name = MailUtil.newId();
        String sender = "sender@localhost";

        MailImpl expected = MailImpl.builder()
            .name(name)
            .sender(sender)
            .build();

        MailImpl mail = MailImpl.builder()
            .name(name)
            .sender(sender)
            .mimeMessage(emptyMessage)
            .build();

        assertThat(mail).isEqualToIgnoringGivenFields(expected, "message", "lastUpdated");
        assertThat(mail.getLastUpdated()).isCloseTo(new Date(), TimeUnit.SECONDS.toMillis(1));
    }

    @Test
    public void mailImplConstructionWithMimeMessageShouldNotOverwriteMessageId() throws MessagingException {
        MailImpl mail = MailImpl.builder()
            .name(MailUtil.newId())
            .sender("sender@localhost")
            .mimeMessage(emptyMessage)
            .build();

        assertThat(mail.getMessage().getMessageID()).isEqualTo(emptyMessage.getMessageID());
    }

    @Test
    public void duplicateFactoryMethodShouldGenerateNewObjectWithSameValuesButName() throws MessagingException, IOException {
        String name = MailUtil.newId();
        MailImpl mail = MailImpl.builder()
            .name(name)
            .sender("sender@localhost")
            .mimeMessage(emptyMessage)
            .build();

        MailImpl duplicate = MailImpl.duplicate(mail);

        assertThat(duplicate).isNotSameAs(mail).isEqualToIgnoringGivenFields(mail, "message", "name");
        assertThat(duplicate.getName()).isNotEqualTo(name);
        assertThat(duplicate.getMessage().getInputStream()).hasSameContentAs(mail.getMessage().getInputStream());
    }

    @Test
    public void setAttributeShouldThrowOnNullAttributeName() throws MessagingException {
        MailImpl mail = newMail();

        assertThatThrownBy(() -> mail.setAttribute(null, "toto"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void deriveNewNameShouldThrowOnNull() {
        assertThatThrownBy(() -> MailImpl.deriveNewName(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void deriveNewNameShouldGenerateNonEmptyStringOnEmpty() throws MessagingException {
        assertThat(MailImpl.deriveNewName("")).isNotEmpty();
    }

    @Test
    public void deriveNewNameShouldNeverGenerateMoreThan86Characters() throws MessagingException {
        String longString = "mu1Eeseemu1Eeseemu1Eeseemu1Eeseemu1Eeseemu1Eeseemu1Eeseemu1Eeseemu1Eeseeseemu1Eesee";
        assertThat(MailImpl.deriveNewName(longString).length()).isLessThan(86);
    }

    @Test
    public void deriveNewNameShouldThrowWhenMoreThan8NestedCalls() throws MessagingException {
        String called6Times = IntStream.range(0, 8)
            .mapToObj(String::valueOf)
            .reduce("average value ", Throwing.binaryOperator((left, right) -> MailImpl.deriveNewName(left)));
        assertThatThrownBy(() -> MailImpl.deriveNewName(called6Times)).isInstanceOf(MessagingException.class);
    }

    @Test
    public void deriveNewNameShouldThrowWhenMoreThan8NestedCallsAndSmallInitialValue() throws MessagingException {
        String called6Times = IntStream.range(0, 8)
            .mapToObj(String::valueOf)
            .reduce("small", Throwing.binaryOperator((left, right) -> MailImpl.deriveNewName(left)));
        assertThatThrownBy(() -> MailImpl.deriveNewName(called6Times)).isInstanceOf(MessagingException.class);
    }

    @Test
    public void deriveNewNameShouldThrowWhenMoreThan8NestedCallsAndLongInitialValue() throws MessagingException {
        String called6Times = IntStream.range(0, 8)
            .mapToObj(String::valueOf)
            .reduce("looooooonnnnnngggggggggggggggg", Throwing.binaryOperator((left, right) -> MailImpl.deriveNewName(left)));
        assertThatThrownBy(() -> MailImpl.deriveNewName(called6Times)).isInstanceOf(MessagingException.class);
    }

    @Test
    public void deriveNewNameShouldGenerateNotEqualsCurrentName() throws MessagingException {
        assertThat(MailImpl.deriveNewName("current")).isNotEqualTo("current");
    }

    @Test
    public void getMaybeSenderShouldHandleNullSender() {
        assertThat(
            MailImpl.builder()
                .name("mail-id")
                .sender(MailAddress.nullSender())
                .build()
                .getMaybeSender())
            .isEqualTo(MaybeSender.nullSender());
    }

    @Test
    public void getMaybeSenderShouldHandleNoSender() {
        assertThat(
            MailImpl.builder()
                .name("mail-id")
                .build()
                .getMaybeSender())
            .isEqualTo(MaybeSender.nullSender());
    }

    @Test
    public void getMaybeSenderShouldHandleSender() {
        assertThat(
            MailImpl.builder()
                .name("mail-id")
                .sender(MailAddressFixture.SENDER)
                .build()
                .getMaybeSender())
            .isEqualTo(MaybeSender.of(MailAddressFixture.SENDER));
    }

    @Test
    public void hasSenderShouldReturnFalseWhenSenderIsNull() {
        assertThat(
            MailImpl.builder()
                .name("mail-id")
                .sender(MailAddress.nullSender())
                .build()
                .hasSender())
            .isFalse();
    }

    @Test
    public void hasSenderShouldReturnFalseWhenSenderIsNotSpecified() {
        assertThat(
            MailImpl.builder()
                .name("mail-id")
                .build()
                .hasSender())
            .isFalse();
    }

    @Test
    public void hasSenderShouldReturnTrueWhenSenderIsSpecified() {
        assertThat(
            MailImpl.builder()
                .name("mail-id")
                .sender(MailAddressFixture.SENDER)
                .build()
                .hasSender())
            .isTrue();
    }

    @Test
    public void builderShouldNotAllowNullName() {
        assertThatThrownBy(() -> MailImpl.builder().name(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void builderShouldNotAllowEmptyName() {
        assertThatThrownBy(() -> MailImpl.builder().name(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void mailImplShouldNotAllowSettingNullName() {
        assertThatThrownBy(() -> newMail().setName(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void mailImplShouldNotAllowSettingEmptyName() {
        assertThatThrownBy(() -> newMail().setName(""))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
