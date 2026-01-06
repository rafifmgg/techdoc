package com.ocmseservice.apiservice.workflows.searchnoticetopay;

import com.ocmseservice.apiservice.crud.beans.CrudResponse;
import com.ocmseservice.apiservice.crud.beans.ResponseMessage;
import com.ocmseservice.apiservice.crud.beans.SingleResponse;
import com.ocmseservice.apiservice.utilities.parkingfines.SearchNoticesUtils;
import com.ocmseservice.apiservice.workflows.searchnoticetopay.dto.SearchParkingNoticeDTO;

import com.ocmseservice.apiservice.workflows.searchnoticetopay.dto.ParkingFinesDTO;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ocmseservice.apiservice.crud.beans.CrudResponse.AppCodes.SUCCESS;
import static com.ocmseservice.apiservice.utilities.ErrorMessageFormatter.formatErrorMessage;

/**
 * Controller for parking fines related operations
 */
@Slf4j
@RestController
@RequestMapping("v1")
public class ParkingFinesController {


    @Autowired
    private SearchNoticesUtils searchNoticesUtils;

    /**
     * POST endpoint for finding parking fines by notice numbers, ID numbers, or vehicle numbers
     *
     * @param payload Map containing search parameters (noticeNo, idNo, or vehicleNo)
     * @return Response containing parking fines information
     */
    @PostMapping("/parkingfines")
    public ResponseEntity<?> findParkingFines(@RequestBody Map<String, Object> payload) {

        try {
            log.info("---PARKING FINES CONTROLLER----"+ " " + payload);
            Object limitVal = payload.get("$limit");
            int limit = (limitVal instanceof Number) ? ((Number) limitVal).intValue() : 10;

            Object skipVal = payload.get("$skip");
            int skip = (skipVal instanceof Number) ? ((Number) skipVal).intValue() : 0;

            // Check which parameter is provided
            if (payload.containsKey("noticeNo")) {
                String noticeNumber = (String) payload.get("noticeNo");

                if (noticeNumber == null || noticeNumber.isEmpty()) {
                    throw new IllegalArgumentException("noticeNo is required and cannot be empty");
                }

                SearchParkingNoticeDTO searchResult2 = searchNoticesUtils.checkNotices(noticeNumber);

                if(searchResult2.getVehicleNo() ==null){
                    String formattedMessage = formatErrorMessage(searchResult2, searchResult2.getErrorMessage());
                    ResponseMessage responseMessage = new ResponseMessage(SUCCESS,formattedMessage);
                    return new ResponseEntity<>(responseMessage, HttpStatus.OK);
                }

                ParkingFinesDTO dto = new ParkingFinesDTO();
                String errorMessage = searchResult2.getErrorMessage();
                //dto.setAppCode(SUCCESS);
                dto.setNoticeNo(searchResult2.getNoticeNo());
                dto.setVehicleNo(searchResult2.getVehicleNo());
                //tidak di tampilkan di pencarian ini
                //dto.setNoticeDateTime(searchResult2.getNoticeDateAndTime());
                dto.setAmountPayable(searchResult2.getAmountPayable());
                //tidk di tampilkan di pencarian ini
               // dto.setPpCode(searchResult2.getPpCode());
                dto.setTransactionDateAndTime(searchResult2.getTransactionDateAndTime());

                if (errorMessage != null) {
                    // Format current date as MM/dd/yyyy
                    String formattedMessage = formatErrorMessage(searchResult2, searchResult2.getErrorMessage());
                    dto.setErrorMessage(formattedMessage);
                }
                dto.setShow(searchResult2.getShow());
                dto.setNoticePaymentFlag(searchResult2.getNoticePaymentFlag());
                List<ParkingFinesDTO> dtoList = List.of(dto);
                int total = dtoList.size();
                SingleResponse<List<ParkingFinesDTO>> response = SingleResponse.createPaginatedResponse(dtoList, total, limit, skip);

                // Return the wrapped result
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else if (payload.containsKey("idNo")) {
                System.out.println("Processing idNo: " + payload.get("idNo"));

                String idNos = (String) payload.get("idNo");

                if (idNos == null || idNos.isEmpty()) {
                    throw new IllegalArgumentException("idNo is required and cannot be empty");
                }

                List<SearchParkingNoticeDTO> searchResults = searchNoticesUtils.findParkingFinesByIdNo(idNos);

                boolean allVehicleNoEmpty = (searchResults.stream().allMatch(dto -> dto.getVehicleNo() == null ));

                if (allVehicleNoEmpty) {
                    SearchParkingNoticeDTO dto = searchResults.get(0);
                    String errorMessage = Optional.ofNullable(searchResults.get(0).getErrorMessage())
                            .orElse("Data Not Found");
                    String formattedMessage = formatErrorMessage(dto, errorMessage);
                    ResponseMessage responseMessage = new ResponseMessage(SUCCESS, formattedMessage);
                    return new ResponseEntity<>(responseMessage, HttpStatus.OK);
                }

                List<ParkingFinesDTO> parkingFinesDTOs = searchResults.stream()
                        .map(result -> {
                            ParkingFinesDTO dto = new ParkingFinesDTO();
                            String errorMessage = result.getErrorMessage();
                            //dto.setAppCode(SUCCESS);
                            dto.setNoticeNo(result.getNoticeNo());
                            dto.setVehicleNo(result.getVehicleNo());
                            dto.setNoticeDateTime(result.getNoticeDateAndTime());
                            dto.setAmountPayable(result.getAmountPayable());
                            dto.setPpCode(result.getPpCode());
                            dto.setTransactionDateAndTime(result.getTransactionDateAndTime());
                            if (errorMessage != null) {
                                // Format current date as MM/dd/yyyy
                                String formattedMessage = formatErrorMessage(result, result.getErrorMessage());
                                dto.setErrorMessage(formattedMessage);
                            }
                            dto.setShow(result.getShow());
                            dto.setNoticePaymentFlag(result.getNoticePaymentFlag());
                            return dto;
                        })
                        .collect(Collectors.toList());

                int total = parkingFinesDTOs.size();
                List<ParkingFinesDTO> paginatedList = parkingFinesDTOs.stream()
                                                            .skip(skip)
                                                            .limit(limit)
                                                            .collect(Collectors.toList());
                SingleResponse<List<ParkingFinesDTO>> response = SingleResponse.createPaginatedResponse(paginatedList,total,limit,skip);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

            else if (payload.containsKey("vehicleNo")) {

                String vehicleNo = (String) payload.get("vehicleNo");

                if (vehicleNo == null || vehicleNo.isEmpty()) {
                    throw new IllegalArgumentException("vehicleNo is required and cannot be empty");
                }

                List<SearchParkingNoticeDTO> searchResults = searchNoticesUtils.findParkingFinesByVehicleNo(vehicleNo);

                boolean allVehicleNoEmpty = (searchResults.stream().allMatch(dto -> dto.getVehicleNo() == null ));

                if (allVehicleNoEmpty) {
                    SearchParkingNoticeDTO dto = searchResults.get(0);

                    String errorMessage = Optional.ofNullable(searchResults.get(0).getErrorMessage())
                            .orElse("Data Not Found");
                    String formattedMessage = formatErrorMessage(dto, errorMessage);
                    ResponseMessage responseMessage = new ResponseMessage(SUCCESS, formattedMessage);
                    return new ResponseEntity<>(responseMessage, HttpStatus.OK);
                }

                // Filter and map the results to ParkingFinesDTO objects
                List<ParkingFinesDTO> parkingFinesDTOs = searchResults.stream()
                        .map(searchResult -> {
//                List<ParkingFinesDTO> parkingFinesDTOs = searchResults.stream()
//                        .filter(searchResult -> !"PAID".equalsIgnoreCase(searchResult.getNoticePaymentFlag()))
//                        .map(searchResult -> {
                            ParkingFinesDTO dto = new ParkingFinesDTO();
                            String errorMessage = searchResult.getErrorMessage();
                            //dto.setAppCode(SUCCESS);
                            dto.setNoticeNo(searchResult.getNoticeNo());
                            dto.setVehicleNo(searchResult.getVehicleNo());
                            //tidak di tampilkan di pencarian ini
                            //dto.setNoticeDateTime(searchResult.getNoticeDateAndTime());
                            dto.setAmountPayable(searchResult.getAmountPayable());
                            //tidak di tampilkan di pencarian ini
                            //dto.setPpCode(searchResult.getPpCode());
                            dto.setTransactionDateAndTime(searchResult.getTransactionDateAndTime());
                            if (errorMessage != null) {
                                String formattedMessage = formatErrorMessage(searchResult, searchResult.getErrorMessage());
                                dto.setErrorMessage(formattedMessage);
                            }

                            dto.setShow(searchResult.getShow());
                            dto.setNoticePaymentFlag(searchResult.getNoticePaymentFlag());
                            return dto;
                        })
                        .collect(Collectors.toList());
                int total = parkingFinesDTOs.size();
                List<ParkingFinesDTO> paginatedList = parkingFinesDTOs.stream()
                                                            .skip(skip)
                                                            .limit(limit)
                                                            .collect(Collectors.toList());

                SingleResponse<List<ParkingFinesDTO>> response = SingleResponse.createPaginatedResponse(paginatedList,total,limit,skip);

                // Return the wrapped result
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            else {
                System.out.println("No valid parameter provided");
                // No valid parameter provided
                return new ResponseEntity<>(
                        CrudResponse.error(
                                CrudResponse.AppCodes.BAD_REQUEST,
                                "At least one of noticeNo, idNo, or vehicleNo must be provided"
                        ),
                        HttpStatus.BAD_REQUEST
                );
            }
        } catch (ClassCastException e) {
            System.out.println("ERROR: " + e.getMessage());
            // Handle case where parameter is not of expected type
            CrudResponse<?> errorResponse = CrudResponse.error(
                    CrudResponse.AppCodes.BAD_REQUEST,
                    "Invalid format for parameters. Expected appropriate types: " + e.getMessage()
            );

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            // Return error response for other exceptions
            CrudResponse<?> errorResponse = CrudResponse.error(
                    CrudResponse.AppCodes.BAD_REQUEST,
                    "Error finding parking fines: " + e.getMessage()
            );

            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
}
