<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:p="info:srw/schema/5/picaXML-v1.0"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:zs="http://www.loc.gov/zing/srw/"
                exclude-result-prefixes="xsl p xlink zs">
    <xsl:mode on-no-match="shallow-copy"/>

    <xsl:import href="default/pica2mods-default-titleInfo.xsl" />
    <xsl:import href="default/pica2mods-default-name.xsl" />
    <xsl:import href="default/pica2mods-default-identifier.xsl" />
    <xsl:import href="default/pica2mods-default-language.xsl" />
    <xsl:import href="default/pica2mods-default-location.xsl" />
    <xsl:import href="default/pica2mods-default-physicalDescription.xsl" />
    <xsl:import href="default/pica2mods-default-originInfo.xsl" />
    <xsl:import href="default/pica2mods-default-genre.xsl" />
    <xsl:import href="default/pica2mods-default-recordInfo.xsl" />
    <xsl:import href="default/pica2mods-default-note.xsl" />
    <xsl:import href="default/pica2mods-default-abstract.xsl" />
    <xsl:import href="default/pica2mods-default-subject.xsl" />
    <xsl:import href="dfi/pica2mods-dfi-relatedItem.xsl" />

    <xsl:import href="_common/pica2mods-pica-PREPROCESSING.xsl" />
    <xsl:import href="_common/pica2mods-functions.xsl" />

    <xsl:param name="MCR.PICA2MODS.CONVERTER_VERSION" select="'Pica2Mods 2.5'" />
    <xsl:param name="MCR.PICA2MODS.DATABASE" select="'gvk'" />
    <xsl:param name="RestrictedAccess" />

    <xsl:template match="p:record">
        <mods:mods>
            <xsl:call-template name="modsTitleInfo" />
            <xsl:call-template name="modsAbstract" />
            <xsl:call-template name="modsName" />
            <xsl:call-template name="modsIdentifier" />
            <xsl:call-template name="modsLanguage" />
            <xsl:call-template name="modsPhysicalDescription" />
            <xsl:call-template name="modsOriginInfo" />
            <xsl:call-template name="modsAccessCondition" />
            <xsl:call-template name="modsGenre" />
            <xsl:call-template name="modsLocation" />
            <xsl:call-template name="modsRecordInfo" />
            <xsl:call-template name="modsNote" />
            <xsl:call-template name="modsRelatedItem" />
            <xsl:call-template name="modsSubject" />
            <xsl:call-template name="modsDfiFIVSubject" />
            <xsl:call-template name="modsDfiSpecific" />
        </mods:mods>
    </xsl:template>

    <xsl:template name="modsAccessCondition">
        <xsl:choose>
          <xsl:when test="$RestrictedAccess = 'true'">
            <mods:accessCondition type="restriction on access" xlink:href="http://www.mycore.org/classifications/mir_access#ipAddressRange" xlink:type="simple"/>
          </xsl:when>
          <xsl:otherwise>
            <mods:accessCondition type="restriction on access" xlink:href="http://www.mycore.org/classifications/mir_access#unlimited" xlink:type="simple"/>
          </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="modsDfiFIVSubject" >
        <xsl:for-each select="./p:datafield[@tag='045N'][not(@occurrence)]" >
            <mods:subject xlink:type="simple">
                <xsl:variable name="valueURI" select="concat('http://bartoc.org/en/node/20447#',translate(./p:subfield[@code='a'][text()!=' '],' ',''))"/>
                <mods:geographic authorityURI="http://bartoc.org/en/node/20447" valueURI="{$valueURI}"/>
            </mods:subject>
        </xsl:for-each>
        <xsl:for-each select="./p:datafield[@tag='045N'][@occurrence='01']" >
            <mods:subject xlink:type="simple">
                <xsl:variable name="valueURI" select="concat('http://bartoc.org/en/node/20446#',translate(./p:subfield[@code='a'][text()!=' '],' ',''))"/>
                <mods:topic authorityURI="http://bartoc.org/en/node/20446" valueURI="{$valueURI}"/>
            </mods:subject>
        </xsl:for-each>
        <xsl:for-each select="./p:datafield[@tag='045N'][@occurrence='02']" >
            <mods:subject xlink:type="simple">
                <xsl:variable name="valueURI" select="concat('http://repo.dfi.de/api/v1/classifications/fivdfi#',translate(./p:subfield[@code='a'],' ',''))"/>
                <mods:topic authorityURI="http://repo.dfi.de/api/v1/classifications/fivdfi" valueURI="{$valueURI}"/>
            </mods:subject>
        </xsl:for-each>
        <xsl:for-each select="./p:datafield[@tag='045D'][@occurrence='60' or @occurrence='70']" >
            <mods:subject xlink:type="simple">
                <mods:topic><xsl:value-of select="./p:subfield[@code='a']"/></mods:topic>
            </mods:subject>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="modsDfiSpecific" >

        <xsl:variable name="pica0500_1" select="substring(./p:datafield[@tag='002@']/p:subfield[@code='0'],2,1)" />
        <xsl:variable name="pica0500_2" select="substring(./p:datafield[@tag='002@']/p:subfield[@code='0'],2,1)" />

        <xsl:choose>
            <xsl:when test="./p:datafield[@tag='037C']">
                <xsl:choose>
                    <xsl:when test="contains(./p:datafield[@tag='037C'][0],'Habil.')
                          or contains(./p:datafield[@tag='037C'][0],'Habilitation')">
                        <mods:genre type="intern" authorityURI="http://www.mycore.org/classifications/mir_genres" valueURI="http://www.mycore.org/classifications/mir_genres#habilitation"/>
                    </xsl:when>
                    <xsl:when test="contains(./p:datafield[@tag='037C'][0],'Diss.')
                          or contains(./p:datafield[@tag='037C'][0],'Dissertation')
                          or contains(./p:datafield[@tag='037C'][0],'Thèse de doctorat')">
                        <mods:genre type="intern" authorityURI="http://www.mycore.org/classifications/mir_genres" valueURI="http://www.mycore.org/classifications/mir_genres#dissertation"/>
                    </xsl:when>
                    <xsl:when test="contains(./p:datafield[@tag='037C'][0],'Dipl.')
                          or contains(./p:datafield[@tag='037C'][0],'Diplom')">
                        <mods:genre type="intern" authorityURI="http://www.mycore.org/classifications/mir_genres" valueURI="http://www.mycore.org/classifications/mir_genres#diploma_thesis"/>
                    </xsl:when>
                    <xsl:when test="contains(./p:datafield[@tag='037C'][0],'Master')">
                        <mods:genre type="intern" authorityURI="http://www.mycore.org/classifications/mir_genres" valueURI="http://www.mycore.org/classifications/mir_genres#master_thesis"/>
                    </xsl:when>
                    <xsl:when test="contains(./p:datafield[@tag='037C'][0],'Bachelor')">
                        <mods:genre type="intern" authorityURI="http://www.mycore.org/classifications/mir_genres" valueURI="http://www.mycore.org/classifications/mir_genres#diploma_bachelor"/>
                    </xsl:when>
                    <xsl:when test="contains(./p:datafield[@tag='037C'][0],'Magister')">
                        <mods:genre type="intern" authorityURI="http://www.mycore.org/classifications/mir_genres" valueURI="http://www.mycore.org/classifications/mir_genres#diploma_thesis"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <mods:genre type="intern" authorityURI="http://www.mycore.org/classifications/mir_genres" valueURI="http://www.mycore.org/classifications/mir_genres#thesis"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="contains('a',$pica0500_2) and $pica0500_1='A'">
                <mods:genre type="intern" authorityURI="http://www.mycore.org/classifications/mir_genres" valueURI="http://www.mycore.org/classifications/mir_genres#book"/>
            </xsl:when>
            <xsl:when test="./p:datafield[@tag='039B']">
                <mods:genre type="intern" authorityURI="http://www.mycore.org/classifications/mir_genres" valueURI="http://www.mycore.org/classifications/mir_genres#article"/>
            </xsl:when>
            <xsl:otherwise>
                <mods:genre type="intern" authorityURI="http://www.mycore.org/classifications/mir_genres" valueURI="http://www.mycore.org/classifications/mir_genres#{$pica0500_2}"/>
            </xsl:otherwise>
        </xsl:choose>
        <mods:accessCondition type="use and reproduction" xlink:href="http://www.mycore.org/classifications/mir_licenses#rights_reserved" xlink:type="simple"/>
    </xsl:template>

    <xsl:template match="zs:searchRetrieveResponse">
        <xsl:apply-templates select="//p:record" />
    </xsl:template>

</xsl:stylesheet>