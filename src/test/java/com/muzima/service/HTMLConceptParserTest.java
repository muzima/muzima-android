package com.muzima.service;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class HTMLConceptParserTest {

    @Test
    public void shouldReturnListOfConcepts() {
        String html = "<html>\n" +
                "<head>\n" +
                "    <link href=\"css/bootstrap.min.css\" rel=\"stylesheet\">\n" +
                "    <link href=\"css/muzima.css\" rel=\"stylesheet\">\n" +
                "    <link href=\"css/ui-darkness/jquery-ui-1.10.4.custom.min.css\" rel=\"stylesheet\">\n" +
                "\n" +
                "    <script src=\"js/jquery.min.js\"></script>\n" +
                "    <script src=\"js/jquery-ui-1.10.4.custom.min.js\"></script>\n" +
                "    <script src=\"js/jquery.validate.min.js\"></script>\n" +
                "    <script src=\"js/additional-methods.min.js\"></script>\n" +
                "    <script src=\"js/muzima.js\"></script>\n" +
                "</head>\n" +
                "<body class=\"col-md-8 col-md-offset-2\">\n" +
                "<div id=\"result\"></div>\n" +
                "<form id=\"registration_form\" name=\"registration_form\">\n" +
                "    <h2 class=\"text-center\">CCSP Histopathology Form</h2>\n" +
                "\n" +
                "    <div class=\"section\">\n" +
                "        <h3>Patient Demographics</h3>\n" +
                "\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"patient.given_name\">Given Name: <span class=\"required\">*</span></label>\n" +
                "            <input class=\"form-control\" id=\"patient.given_name\" name=\"patient.given_name\" type=\"text\"\n" +
                "                   required=\"required\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"patient.family_name\">Family Name: <span class=\"required\">*</span> </label>\n" +
                "            <input class=\"form-control\" id=\"patient.family_name\" name=\"patient.family_name\" type=\"text\"\n" +
                "                   required=\"required\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"patient.middle_name\">Middle Name:</label>\n" +
                "            <input class=\"form-control\" id=\"patient.middle_name\" name=\"patient.middle_name\" type=\"text\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"patient.medical_record_number\">AMRS ID Number</label>\n" +
                "            <input class=\"form-control checkDigit\" id=\"patient.medical_record_number\"\n" +
                "                   name=\"patient.medical_record_number\" type=\"text\" required=\"required\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"patient.sex\">Gender: <span class=\"required\">*</span></label>\n" +
                "            <select class=\"form-control\" id=\"patient.sex\" name=\"patient.sex\" required=\"required\">\n" +
                "                <option value=\"\">...</option>\n" +
                "                <option value=\"M\">Male</option>\n" +
                "                <option value=\"F\">Female</option>\n" +
                "            </select>\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"patient.birthdate\">Date Of Birth</label>\n" +
                "            <input class=\"form-control datepicker\" id=\"patient.birthdate\"\n" +
                "                   name=\"patient.birthdate\" type=\"text\" required=\"required\" readonly=\"readonly\">\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    <div class=\"section repeat\">\n" +
                "        <h3>Procedures Done This Visit</h3>\n" +
                "\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"obs.body_part\">Body Part Where Specimen Was Collected? <span class=\"required\">*</span></label>\n" +
                "            <select class=\"form-control record-concept\" name=\"8265^BODY PART^99DCT\" id=\"obs.body_part\" required=\"required\">\n" +
                "                <option value=\"\">...</option>\n" +
                "                <option value=\"8279^CERVIX^99DCT\">Cervix</option>\n" +
                "                <option value=\"8280^VAGINA^99DCT\">Vagina</option>\n" +
                "                <option value=\"8281^VULVA^99DCT\">Vulva</option>\n" +
                "            </select>\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"obs.procedures_done_this_visit_detailed\">How was specimen collected?</label>\n" +
                "            <select class=\"form-control record-concept\" name=\"7479^PROCEDURES DONE THIS VISIT^99DCT\"\n" +
                "                    id=\"obs.procedures_done_this_visit_detailed\" required=\"required\">\n" +
                "                <option value=\"\">...</option>\n" +
                "                <option value=\"7147^LOOP ELECTROSURGICAL EXCISION PROCEDURE^99DCT\">LEEP</option>\n" +
                "                <option value=\"6511^EXCISIONAL OR SURGICAL BIOPSY^99DCT\">Cervical punch/excisional Biopsy</option>\n" +
                "                <option value=\"7478^ENDOCERVICAL CURETTAGE ^99DCT\">Endocervical curretage (ECC)</option>\n" +
                "            </select>\n" +
                "        </div>\n" +
                "        <div class=\"form-group \">\n" +
                "            <label for=\"obs.anatomic_location_description\">Provide Location Of Specimen</label>\n" +
                "            <select class=\"form-control record-concept\" name=\"8268^ANATOMIC LOCATION DESCRIPTION^99DCT\" id=\"obs.anatomic_location_description\"\n" +
                "                    required=\"required\">\n" +
                "                <option value=\"\">...</option>\n" +
                "                <option value=\"8266^SUPERFICIAL^99DCT\">Superficial</option>\n" +
                "                <option value=\"8267^DEEP^99DCT\">Deep</option>\n" +
                "                <option value=\"5622^OTHER NON-CODED^99DCT\">Other non-coded</option>\n" +
                "            </select>\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"clock_face_cervical_biopsy_location\">Provide location where biopsy for specimen was done:\n" +
                "                __O'clock</label>\n" +
                "            <input class=\"form-control checkDigit record-concept\" id=\"clock_face_cervical_biopsy_location\"\n" +
                "                   name=\"7481^CLOCK FACE CERVICAL BIOPSY LOCATION ^99DCT\" type=\"number\">\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"obs.pathological_diagnosis_added_cervix\">Pathological Diagnosis for Cervical Specimen</label>\n" +
                "            <select class=\"form-control record-concept\" name=\"8278^PATHOLOGICAL DIAGNOSIS ADDED^99DCT\"\n" +
                "                    id=\"obs.pathological_diagnosis_added_cervix\" required=\"required\">\n" +
                "                <option>...</option>\n" +
                "                <option value=\"1115^NORMAL^99DCT\">Normal</option>\n" +
                "                <option value=\"149^CERVICITIS^99DCT\">Cervicitis</option>\n" +
                "                <option value=\"8282^CERVICAL SQUAMOUS METAPLASIA^99DCT\">Cervical squamous metaplasia</option>\n" +
                "                <option value=\"7424^CERVICAL INTRAEPITHELIAL NEOPLASIA GRADE 1^99DCT\">Mild cervical intraepithelial\n" +
                "                    neoplasia (CIN 1)\n" +
                "                </option>\n" +
                "                <option value=\"7425^CERVICAL INTRAEPITHELIAL NEOPLASIA GRADE 2^99DCT\">Moderate cervical intraepithelial\n" +
                "                    neoplasia (CIN 2)\n" +
                "                </option>\n" +
                "                <option value=\"7216^CERVICAL INTRAEPITHELIAL NEOPLASIA GRADE 3^99DCT\">Severe cervical intraepithelial\n" +
                "                    neoplasia (CIN 3)\n" +
                "                </option>\n" +
                "                <option value=\"8275^CERVICAL ADENOCARCINOMA^99DCT\">Cervical adenocarcinoma</option>\n" +
                "                <option value=\"8275^CERVICAL ADENOCARCINOMA^99DCT\">Cervical squamous cell carcinoma</option>\n" +
                "                <option value=\"5622^OTHER NON-CODED^99DCT\">Other non-coded</option>\n" +
                "            </select>\n" +
                "        </div>\n" +
                "        <div class=\"form-group concept\">\n" +
                "            <label for=\"obs.pathological_diagnosis_added_vagina\">Pathological Diagnosis for Vaginal Specimen</label>\n" +
                "            <select class=\"form-control record-concept\" name=\"8278^PATHOLOGICAL DIAGNOSIS ADDED^99DCT\"\n" +
                "                    id=\"obs.pathological_diagnosis_added_vagina\" required=\"required\">\n" +
                "                <option>...</option>\n" +
                "                <option value=\"1115^NORMAL^99DCT\">Normal</option>\n" +
                "                <option value=\"7492^VAGINAL INTRAEPITHELIAL NEOPLASIA GRADE 1^99DCT\">Mild vaginal intraepithelial\n" +
                "                    neoplasia (VAIN1)\n" +
                "                </option>\n" +
                "                <option value=\"7491^VAGINAL INTRAEPITHELIAL NEOPLASIA GRADE 2 ^99DCT\">Moderate vaginal intraepithelial\n" +
                "                    neoplasia (VAIN 2)\n" +
                "                </option>\n" +
                "                <option value=\"7435^VAGINAL INTRAEPITHELIAL NEOPLASIA GRADE 3^99DCT\">Severe vaginal intraepithelial\n" +
                "                    neoplasia (VAIN 3)\n" +
                "                </option>\n" +
                "                <option value=\"5622^OTHER NON-CODED^99DCT\">Other non-coded</option>\n" +
                "            </select>\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"obs.pathological_diagnosis_added_vulva\">Pathological Diagnosis for Vulval Specimen</label>\n" +
                "            <select class=\"form-control record-concept\" name=\"8278^PATHOLOGICAL DIAGNOSIS ADDED^99DCT\"\n" +
                "                    id=\"obs.pathological_diagnosis_added_vulva\" required=\"required\">\n" +
                "                <option>...</option>\n" +
                "                <option value=\"1115^NORMAL^99DCT\">Normal</option>\n" +
                "                <option value=\"7489^CONDYLOMA OR VULVAR INTRAEPITHELIAL NEOPLASIA GRADE 1^99DCT\">Condyloma / Mild vulva\n" +
                "                    intraepithelial neoplasia (VIN1)\n" +
                "                </option>\n" +
                "                <option value=\"7488^VULVAR INTRAEPITHELIAL NEOPLASIA GRADE 2^99DCT\">Moderate vulva intraepithelial\n" +
                "                    neoplasia (VIN 2)\n" +
                "                </option>\n" +
                "                <option value=\"7483^VULVAR INTRAEPITHELIAL NEOPLASIA GRADE 3^99DCT\">Severe vulva intraepithelial\n" +
                "                    neoplasia (VIN 3)\n" +
                "                </option>\n" +
                "                <option value=\"5622^OTHER NON-CODED^99DCT\">Other non-coded</option>\n" +
                "            </select>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"section\">\n" +
                "\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"freetext_general\">Comments</label>\n" +
                "            <textarea class=\"form-control\" name=\"freetext_general\" id=\"freetext_general\"\n" +
                "                      placeholder=\"Provider notes/comments.\"></textarea>\n" +
                "        </div>\n" +
                "        <div class=\"form-group\">\n" +
                "            <label for=\"return_visit_date\">Encounter Date <span class=\"required\">*</span></label>\n" +
                "            <input class=\"form-control datepicker\" readonly=\"readonly\" id=\"return_visit_date\"\n" +
                "                   name=\"return_visit_date\"\n" +
                "                   type=\"text\" required=\"required\">\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class=\"form-group text-center\">\n" +
                "        <input class=\"btn btn-primary\" type=\"button\" value=\"Save\"/>\n" +
                "        <input class=\"btn btn-primary\" type=\"submit\" value=\"Submit\"/>\n" +
                "    </div>\n" +
                "</form>\n" +
                "\n" +
                "</body>\n" +
                "<script type=\"text/javascript\">\n" +
                "    $(document).ready(function () {\n" +
                "        var showBodyPartSpecificDiagonosis = function (bodyPart) {\n" +
                "            var availableBodyParts = {'8279^CERVIX^99DCT': '#obs\\\\.pathological_diagnosis_added_cervix',\n" +
                "                '8280^VAGINA^99DCT': '#obs\\\\.pathological_diagnosis_added_vagina',\n" +
                "                '8281^VULVA^99DCT': '#obs\\\\.pathological_diagnosis_added_vulva'};\n" +
                "\n" +
                "            $.each(availableBodyParts, function (k, v) {\n" +
                "                if (k == bodyPart) {\n" +
                "                    $(v).parent().show();\n" +
                "                } else {\n" +
                "                    $(v).val('');\n" +
                "                    $(v).parent().hide();\n" +
                "                }\n" +
                "            })\n" +
                "        };\n" +
                "        var $bodyPart = $('#obs\\\\.body_part');\n" +
                "        $bodyPart.change(function () {\n" +
                "            showBodyPartSpecificDiagonosis($bodyPart.val());\n" +
                "        });\n" +
                "        $bodyPart.trigger('change');\n" +
                "\n" +
                "        var $obs = $('#obs\\\\.procedures_done_this_visit_detailed');\n" +
                "        $obs.change(function () {\n" +
                "            if ($('#obs\\\\.procedures_done_this_visit_detailed').val() == \"7147^LOOP ELECTROSURGICAL EXCISION PROCEDURE^99DCT\") {\n" +
                "                $('#obs\\\\.anatomic_location_description').parent().show();\n" +
                "                $('#clock_face_cervical_biopsy_location').parent().hide();\n" +
                "            } else {\n" +
                "                $('#clock_face_cervical_biopsy_location').parent().show();\n" +
                "                $('#obs\\\\.anatomic_location_description').parent().hide();\n" +
                "            }\n" +
                "        });\n" +
                "    });\n" +
                "</script>\n" +
                "\n" +
                "</html>";

        List<String> concepts = new HTMLConceptParser().parse(html);
        assertThat(concepts.size(),is(5));
        assertThat(concepts,hasItem("8265^BODY PART^99DCT"));
        assertThat(concepts,hasItem("7479^PROCEDURES DONE THIS VISIT^99DCT"));
        assertThat(concepts,hasItem("8268^ANATOMIC LOCATION DESCRIPTION^99DCT"));
        assertThat(concepts,hasItem("7481^CLOCK FACE CERVICAL BIOPSY LOCATION ^99DCT"));
        assertThat(concepts,hasItem("8278^PATHOLOGICAL DIAGNOSIS ADDED^99DCT"));
    }
}
